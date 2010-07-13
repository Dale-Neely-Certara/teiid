/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */

package org.teiid.query.sql.visitor;

import static org.teiid.language.SQLConstants.Reserved.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.teiid.core.types.DataTypeManager;
import org.teiid.core.util.StringUtil;
import org.teiid.language.SQLConstants;
import org.teiid.language.SQLConstants.NonReserved;
import org.teiid.language.SQLConstants.Tokens;
import org.teiid.query.sql.LanguageObject;
import org.teiid.query.sql.LanguageVisitor;
import org.teiid.query.sql.lang.AtomicCriteria;
import org.teiid.query.sql.lang.BetweenCriteria;
import org.teiid.query.sql.lang.Command;
import org.teiid.query.sql.lang.CompareCriteria;
import org.teiid.query.sql.lang.CompoundCriteria;
import org.teiid.query.sql.lang.Create;
import org.teiid.query.sql.lang.Criteria;
import org.teiid.query.sql.lang.Delete;
import org.teiid.query.sql.lang.DependentSetCriteria;
import org.teiid.query.sql.lang.Drop;
import org.teiid.query.sql.lang.DynamicCommand;
import org.teiid.query.sql.lang.ExistsCriteria;
import org.teiid.query.sql.lang.ExpressionCriteria;
import org.teiid.query.sql.lang.From;
import org.teiid.query.sql.lang.FromClause;
import org.teiid.query.sql.lang.GroupBy;
import org.teiid.query.sql.lang.Insert;
import org.teiid.query.sql.lang.Into;
import org.teiid.query.sql.lang.IsNullCriteria;
import org.teiid.query.sql.lang.JoinPredicate;
import org.teiid.query.sql.lang.JoinType;
import org.teiid.query.sql.lang.Limit;
import org.teiid.query.sql.lang.MatchCriteria;
import org.teiid.query.sql.lang.NotCriteria;
import org.teiid.query.sql.lang.Option;
import org.teiid.query.sql.lang.OrderBy;
import org.teiid.query.sql.lang.OrderByItem;
import org.teiid.query.sql.lang.PredicateCriteria;
import org.teiid.query.sql.lang.Query;
import org.teiid.query.sql.lang.QueryCommand;
import org.teiid.query.sql.lang.SPParameter;
import org.teiid.query.sql.lang.Select;
import org.teiid.query.sql.lang.SetClause;
import org.teiid.query.sql.lang.SetClauseList;
import org.teiid.query.sql.lang.SetCriteria;
import org.teiid.query.sql.lang.SetQuery;
import org.teiid.query.sql.lang.StoredProcedure;
import org.teiid.query.sql.lang.SubqueryCompareCriteria;
import org.teiid.query.sql.lang.SubqueryFromClause;
import org.teiid.query.sql.lang.SubquerySetCriteria;
import org.teiid.query.sql.lang.TextTable;
import org.teiid.query.sql.lang.UnaryFromClause;
import org.teiid.query.sql.lang.Update;
import org.teiid.query.sql.lang.XMLTable;
import org.teiid.query.sql.lang.TextTable.TextColumn;
import org.teiid.query.sql.lang.XMLTable.XMLColumn;
import org.teiid.query.sql.proc.AssignmentStatement;
import org.teiid.query.sql.proc.Block;
import org.teiid.query.sql.proc.BreakStatement;
import org.teiid.query.sql.proc.CommandStatement;
import org.teiid.query.sql.proc.ContinueStatement;
import org.teiid.query.sql.proc.CreateUpdateProcedureCommand;
import org.teiid.query.sql.proc.CriteriaSelector;
import org.teiid.query.sql.proc.DeclareStatement;
import org.teiid.query.sql.proc.HasCriteria;
import org.teiid.query.sql.proc.IfStatement;
import org.teiid.query.sql.proc.LoopStatement;
import org.teiid.query.sql.proc.RaiseErrorStatement;
import org.teiid.query.sql.proc.Statement;
import org.teiid.query.sql.proc.TranslateCriteria;
import org.teiid.query.sql.proc.WhileStatement;
import org.teiid.query.sql.symbol.AggregateSymbol;
import org.teiid.query.sql.symbol.AliasSymbol;
import org.teiid.query.sql.symbol.AllInGroupSymbol;
import org.teiid.query.sql.symbol.AllSymbol;
import org.teiid.query.sql.symbol.CaseExpression;
import org.teiid.query.sql.symbol.Constant;
import org.teiid.query.sql.symbol.DerivedColumn;
import org.teiid.query.sql.symbol.ElementSymbol;
import org.teiid.query.sql.symbol.Expression;
import org.teiid.query.sql.symbol.ExpressionSymbol;
import org.teiid.query.sql.symbol.Function;
import org.teiid.query.sql.symbol.GroupSymbol;
import org.teiid.query.sql.symbol.QueryString;
import org.teiid.query.sql.symbol.Reference;
import org.teiid.query.sql.symbol.ScalarSubquery;
import org.teiid.query.sql.symbol.SearchedCaseExpression;
import org.teiid.query.sql.symbol.SelectSymbol;
import org.teiid.query.sql.symbol.SingleElementSymbol;
import org.teiid.query.sql.symbol.XMLAttributes;
import org.teiid.query.sql.symbol.XMLElement;
import org.teiid.query.sql.symbol.XMLForest;
import org.teiid.query.sql.symbol.XMLNamespaces;
import org.teiid.query.sql.symbol.XMLParse;
import org.teiid.query.sql.symbol.XMLQuery;
import org.teiid.query.sql.symbol.XMLSerialize;
import org.teiid.query.sql.symbol.XMLNamespaces.NamespaceItem;
import org.teiid.translator.SourceSystemFunctions;

/**
 * <p>The SQLStringVisitor will visit a set of language objects and return the
 * corresponding SQL string representation. </p>
 */
public class SQLStringVisitor extends LanguageVisitor {

    public static final String UNDEFINED = "<undefined>"; //$NON-NLS-1$
    private static final String SPACE = " "; //$NON-NLS-1$
    private static final String BEGIN_COMMENT = "/*"; //$NON-NLS-1$
    private static final String END_COMMENT = "*/"; //$NON-NLS-1$
    private static final char ID_ESCAPE_CHAR = '\"';
    
    private LinkedList<Object> parts = new LinkedList<Object>();

    /**
     * Helper to quickly get the parser string for an object using the visitor.
     * @param obj Language object
     * @return String SQL String for obj
     */
    public static final String getSQLString(LanguageObject obj) {
        if(obj == null) {
            return UNDEFINED; 
        }
        SQLStringVisitor visitor = new SQLStringVisitor();       
        obj.acceptVisitor(visitor);
    	return visitor.getSQLString();
    }

    /**
     * Retrieve completed string from the visitor.
     * @return Complete SQL string for the visited nodes
     */
    public String getSQLString() {
        StringBuilder output = new StringBuilder();
        getSQLString(this.parts, output);
        return output.toString();
    }
    
    public static void getSQLString(List<Object> parts, StringBuilder output) {
        for (Object object : parts) {
            if (object instanceof List) {
                getSQLString((List<Object>)object, output);
            } else {
                output.append(object);
            }
        } 
    }
    
    public List<Object> registerNode(LanguageObject obj) {
        if (obj == null) {
            return Arrays.asList((Object)UNDEFINED);
        }
        SQLStringVisitor visitor = new SQLStringVisitor();       
        obj.acceptVisitor(visitor);
        return visitor.parts;
    }
    
    public void replaceStringParts(Object[] parts) {
        for (int i = 0; i < parts.length; i++) {
            this.parts.add(parts[i]);
        } 
    }

    // ############ Visitor methods for language objects ####################

    public void visit(BetweenCriteria obj) {
        parts.add(registerNode(obj.getExpression()));
        parts.add(SPACE);
        
        if (obj.isNegated()) {
            parts.add(NOT);
            parts.add(SPACE);
        }
        parts.add(BETWEEN);
        parts.add(SPACE);
        parts.add(registerNode(obj.getLowerExpression()));

        parts.add(SPACE);
        parts.add(AND);
        parts.add(SPACE);
        parts.add(registerNode(obj.getUpperExpression()));
    }

    public void visit(CaseExpression obj) {
        parts.add(CASE);
        parts.add(SPACE);
        parts.add(registerNode(obj.getExpression()) ); 
        parts.add(SPACE);

        for (int i = 0; i < obj.getWhenCount(); i++) {
            parts.add(WHEN);
            parts.add(SPACE);
            parts.add(registerNode(obj.getWhenExpression(i)) );
            parts.add(SPACE);
            parts.add(THEN);
            parts.add(SPACE);
            parts.add(registerNode(obj.getThenExpression(i)));
            parts.add(SPACE);
        }

        if (obj.getElseExpression() != null) {
            parts.add(ELSE);
            parts.add(SPACE);
            parts.add(registerNode(obj.getElseExpression()));
            parts.add(SPACE);
        }
        parts.add(END);
    }

    public void visit(CompareCriteria obj) {
        Expression leftExpression = obj.getLeftExpression();
        Object leftPart = registerNode(leftExpression);

        String operator = obj.getOperatorAsString();

        Expression rightExpression = obj.getRightExpression();
        Object rightPart = registerNode(rightExpression);

        replaceStringParts(new Object[] { leftPart, SPACE, operator, SPACE, rightPart });
    }

    public void visit(CompoundCriteria obj) {
        // Get operator string
        int operator = obj.getOperator();
        String operatorStr = ""; //$NON-NLS-1$
        if(operator == CompoundCriteria.AND) {
            operatorStr = AND;
        } else if(operator == CompoundCriteria.OR) {
            operatorStr = OR;
        }

        // Get criteria
        List subCriteria = obj.getCriteria();

        // Build parts
        if(subCriteria.size() == 1) {
            // Special case - should really never happen, but we are tolerant
            Criteria firstChild = (Criteria) subCriteria.get(0);
            replaceStringParts(new Object[] { registerNode(firstChild) });
        } else {
            // Magic formula - suppose you have 2 sub criteria, then the string
            // has parts: (|x|)| |AND| |(|y|)
            // Each sub criteria has 3 parts and each connector has 3 parts
            // Number of connectors = number of sub criteria - 1
            // # parts = 3n + 3c      ; c=n-1
            //         = 3n + 3(n-1)
            //         = 6n - 3
            Object[] parts = new Object[(6*subCriteria.size())-3];

            // Add first criteria
            Iterator iter = subCriteria.iterator();
            Criteria crit = (Criteria) iter.next();
            parts[0] = "("; //$NON-NLS-1$
            parts[1] = registerNode(crit);
            parts[2] = ")"; //$NON-NLS-1$

            // Add rest of the criteria
            for(int i=3; iter.hasNext(); i=i+6) {
                // Add connector
                parts[i] = SPACE;
                parts[i+1] = operatorStr;
                parts[i+2] = SPACE;

                // Add criteria
                crit = (Criteria) iter.next();
                parts[i+3] = "("; //$NON-NLS-1$
                parts[i+4] = registerNode(crit);
                parts[i+5] = ")"; //$NON-NLS-1$
            }

            replaceStringParts(parts);
        }
    }

    public void visit(Delete obj) {
		//add delete clause
		parts.add(DELETE);
		parts.add(SPACE);
		//add from clause
		parts.add(FROM);
		parts.add(SPACE);
		parts.add(registerNode(obj.getGroup()));

		//add where clause
		if(obj.getCriteria() != null) {
			parts.add(SPACE);
			parts.add(WHERE);
			parts.add(SPACE);
			parts.add(registerNode(obj.getCriteria()));
		}

		// Option clause
		if(obj.getOption() != null) {
            parts.add(SPACE);
            parts.add(registerNode(obj.getOption()));
		}
    }
    
    public void visit(DependentSetCriteria obj) {
        parts.add(registerNode(obj.getExpression()));

        // operator and beginning of list
        parts.add(SPACE);
        if (obj.isNegated()) {
            parts.add(NOT);
            parts.add(SPACE);
        }
        parts.add(IN);
        parts.add(" (<dependent values>)"); //$NON-NLS-1$
    }

    public void visit(From obj) {
        Object[] parts = null;
        List clauses = obj.getClauses();
        if(clauses.size() == 1) {
            replaceStringParts(new Object[] {
                FROM, SPACE,
                registerNode( (FromClause) clauses.get(0) ) });
        } else if(clauses.size() > 1) {
            parts = new Object[2 + clauses.size() + (clauses.size()-1)];

            // Add first clause
            parts[0] = FROM;
            parts[1] = SPACE;
            Iterator clauseIter = clauses.iterator();
            parts[2] = registerNode((FromClause) clauseIter.next());

            // Add rest of the clauses
            for(int i=3; clauseIter.hasNext(); i=i+2) {
                parts[i] = ", "; //$NON-NLS-1$
                parts[i+1] = registerNode((FromClause) clauseIter.next());
            }

            replaceStringParts(parts);
        } else {
            // Shouldn't happen, but being tolerant
            replaceStringParts(new Object[] { FROM });
        }
    }

    public void visit(GroupBy obj) {
        Object[] parts = null;
        List symbols = obj.getSymbols();
        if(symbols.size() == 1) {
            replaceStringParts(new Object[] {
                GROUP, SPACE, BY, SPACE,
                registerNode( (Expression) symbols.get(0) ) });
        } else if(symbols.size() > 1) {
            parts = new Object[4 + symbols.size() + (symbols.size()-1)];

            // Add first clause
            parts[0] = GROUP;
            parts[1] = SPACE;
            parts[2] = BY;
            parts[3] = SPACE;
            Iterator symbolIter = symbols.iterator();
            parts[4] = registerNode((Expression) symbolIter.next());

            // Add rest of the clauses
            for(int i=5; symbolIter.hasNext(); i=i+2) {
                parts[i] = ", "; //$NON-NLS-1$
                parts[i+1] = registerNode((Expression) symbolIter.next());
            }

            replaceStringParts(parts);
        } else {
            // Shouldn't happen, but being tolerant
            replaceStringParts(new Object[] { GROUP, SPACE, BY });
        }
    }

    public void visit(Insert obj) {
        formatBasicInsert(obj);
        
        if ( obj.getQueryExpression() != null ) {
            parts.add(registerNode(obj.getQueryExpression()));
        } else {
            parts.add(VALUES);
            parts.add(" ("); //$NON-NLS-1$
            Iterator valueIter = obj.getValues().iterator();
            while(valueIter.hasNext()) {
                Expression valObj = (Expression) valueIter.next();
                parts.add(registerNode(valObj));
                if(valueIter.hasNext()) {
                    parts.add(", "); //$NON-NLS-1$
                }
            }
            parts.add(")"); //$NON-NLS-1$
        }
            
    	// Option clause
		if(obj.getOption() != null) {
            parts.add(SPACE);
            parts.add(registerNode(obj.getOption()));
		}
    }

    public void visit(Create obj) {
        parts.add(CREATE);
        parts.add(SPACE);
        parts.add(LOCAL);
        parts.add(SPACE);
        parts.add(TEMPORARY);
        parts.add(SPACE);
        parts.add(TABLE);
        parts.add(SPACE);
        parts.add(registerNode(obj.getTable()));
        parts.add(SPACE);

        // Columns clause
        List<ElementSymbol> columns = obj.getColumns();
        parts.add("("); //$NON-NLS-1$
        Iterator<ElementSymbol> iter = columns.iterator();
        while(iter.hasNext()) {
            ElementSymbol element = iter.next();
            element.setDisplayMode(ElementSymbol.DisplayMode.SHORT_OUTPUT_NAME);
            parts.add(registerNode(element));
            parts.add(SPACE);
            parts.add(DataTypeManager.getDataTypeName(element.getType()));
            if(iter.hasNext()) {
                parts.add(", "); //$NON-NLS-1$
            }
        }
        parts.add(")"); //$NON-NLS-1$
    }
    
    public void visit(Drop obj) {
        parts.add(DROP);
        parts.add(SPACE);
        parts.add(TABLE);
        parts.add(SPACE);
        parts.add(registerNode(obj.getTable()));
    }
    
    private void formatBasicInsert(Insert obj) {
        parts.add(INSERT);
        parts.add(SPACE);
        parts.add(INTO);
        parts.add(SPACE);
        parts.add(registerNode(obj.getGroup()));
        parts.add(SPACE);
        
        if (!obj.getVariables().isEmpty()) {
            
            // Columns clause
            List vars = obj.getVariables();
            if(vars != null) {
                parts.add("("); //$NON-NLS-1$
                Iterator iter = vars.iterator();
                while(iter.hasNext()) {
                    ElementSymbol element = (ElementSymbol) iter.next();
                    parts.add(registerNode(element));
                    if(iter.hasNext()) {
                        parts.add(", "); //$NON-NLS-1$
                    }
                }
                parts.add(") "); //$NON-NLS-1$
            }
        }
    }

    public void visit(IsNullCriteria obj) {
        Expression expr = obj.getExpression();
        Object exprPart = registerNode(expr);
        parts.add(exprPart);
        parts.add(SPACE);
        parts.add(IS);
        parts.add(SPACE);
        if (obj.isNegated()) {
            parts.add(NOT);
            parts.add(SPACE);
        }
        parts.add(NULL);
    }

    public void visit(JoinPredicate obj) {
        addOptionComment(obj);
        
        if(obj.hasHint()) {
            parts.add("(");//$NON-NLS-1$
        }

        // left clause
        FromClause leftClause = obj.getLeftClause();
		if(leftClause instanceof JoinPredicate && !((JoinPredicate)leftClause).hasHint()) {
			parts.add("("); //$NON-NLS-1$
			parts.add(registerNode(leftClause));
			parts.add(")"); //$NON-NLS-1$
		} else {
			parts.add(registerNode(leftClause));
		}

        // join type
        parts.add(SPACE);
        parts.add(registerNode(obj.getJoinType()));
        parts.add(SPACE);

        // right clause
        FromClause rightClause = obj.getRightClause();
		if(rightClause instanceof JoinPredicate && !((JoinPredicate)rightClause).hasHint()) {
			parts.add("("); //$NON-NLS-1$
			parts.add(registerNode(rightClause));
			parts.add(")"); //$NON-NLS-1$
		} else {
			parts.add(registerNode(rightClause));
		}

        // join criteria
        List joinCriteria = obj.getJoinCriteria();
		if(joinCriteria != null && joinCriteria.size() > 0) {
            parts.add(SPACE);
			parts.add(ON);
            parts.add(SPACE);
			Iterator critIter = joinCriteria.iterator();
			while(critIter.hasNext()) {
				Criteria crit = (Criteria) critIter.next();
                if(crit instanceof PredicateCriteria || crit instanceof AtomicCriteria) {
    				parts.add(registerNode(crit));
                } else {
                    parts.add("("); //$NON-NLS-1$
                    parts.add(registerNode(crit));
                    parts.add(")"); //$NON-NLS-1$
                }

				if(critIter.hasNext()) {
					parts.add(SPACE);
					parts.add(AND);
					parts.add(SPACE);
				}
			}
		}

        if(obj.hasHint()) {
            parts.add(")"); //$NON-NLS-1$
        }
        addFromClasueDepOptions(obj);
    }

    private void addFromClasueDepOptions(FromClause obj) {
        if (obj.isMakeDep()) {
            parts.add(SPACE);
            parts.add(Option.MAKEDEP);
        }
        if (obj.isMakeNotDep()) {
            parts.add(SPACE);
            parts.add(Option.MAKENOTDEP);
        }
    }

    private void addOptionComment(FromClause obj) {
    	if (obj.isOptional()) {
	    	parts.add(BEGIN_COMMENT);
	        parts.add(SPACE);
	        parts.add(Option.OPTIONAL);
	        parts.add(SPACE);
	        parts.add(END_COMMENT);
	        parts.add(SPACE);
    	}
    }

    public void visit(JoinType obj) {
        Object[] parts = null;
        if(obj.equals(JoinType.JOIN_INNER)) {
            parts = new Object[] { INNER, SPACE, JOIN };
        } else if(obj.equals(JoinType.JOIN_CROSS)) {
            parts = new Object[] { CROSS, SPACE, JOIN };
        } else if(obj.equals(JoinType.JOIN_LEFT_OUTER)) {
            parts = new Object[] { LEFT, SPACE, OUTER, SPACE, JOIN };
        } else if(obj.equals(JoinType.JOIN_RIGHT_OUTER)) {
            parts = new Object[] { RIGHT, SPACE, OUTER, SPACE, JOIN };
        } else if(obj.equals(JoinType.JOIN_FULL_OUTER)) {
            parts = new Object[] { FULL, SPACE, OUTER, SPACE, JOIN };
        } else if(obj.equals(JoinType.JOIN_UNION)) {
            parts = new Object[] { UNION, SPACE, JOIN };
        } else if (obj.equals(JoinType.JOIN_SEMI)) {
            parts = new Object[] { "SEMI", SPACE, JOIN }; //$NON-NLS-1$
        } else if (obj.equals(JoinType.JOIN_ANTI_SEMI)) {
            parts = new Object[] { "ANTI SEMI", SPACE, JOIN }; //$NON-NLS-1$
        }

        replaceStringParts(parts);
    }

    public void visit(MatchCriteria obj) {
        parts.add(registerNode(obj.getLeftExpression()));

        parts.add(SPACE);
        if (obj.isNegated()) {
            parts.add(NOT);
            parts.add(SPACE);
        }
        parts.add(LIKE);
        parts.add(SPACE);

        parts.add(registerNode(obj.getRightExpression()));

        if(obj.getEscapeChar() != MatchCriteria.NULL_ESCAPE_CHAR) {
            parts.add(SPACE);
            parts.add(ESCAPE);
            parts.add(" '"); //$NON-NLS-1$
            parts.add("" + obj.getEscapeChar()); //$NON-NLS-1$
            parts.add("'"); //$NON-NLS-1$
        }
    }

    public void visit(NotCriteria obj) {
        parts.add(NOT);
        parts.add(" ("); //$NON-NLS-1$
        parts.add(registerNode(obj.getCriteria()));
        parts.add(")"); //$NON-NLS-1$
    }

    public void visit(Option obj) {
        parts.add(OPTION);

        Collection groups = obj.getDependentGroups();
        if(groups != null && groups.size() > 0) {
            parts.add(" "); //$NON-NLS-1$
            parts.add(MAKEDEP);
            parts.add(" "); //$NON-NLS-1$

            Iterator iter = groups.iterator();

            while(iter.hasNext()) {
                outputDisplayName((String)iter.next());
                
                if (iter.hasNext()) {
                	parts.add(", ");
                }
            }
        }
        
        groups = obj.getNotDependentGroups();
        if(groups != null && groups.size() > 0) {
            parts.add(" "); //$NON-NLS-1$
            parts.add(MAKENOTDEP);
            parts.add(" "); //$NON-NLS-1$

            Iterator iter = groups.iterator();

            while(iter.hasNext()) {
                outputDisplayName((String)iter.next());
                
                if (iter.hasNext()) {
                	parts.add(", ");
                }
            }
        }
        
        groups = obj.getNoCacheGroups();
        if(groups != null && groups.size() > 0) {
            parts.add(" "); //$NON-NLS-1$
            parts.add(NOCACHE);
            parts.add(" "); //$NON-NLS-1$

            Iterator iter = groups.iterator();

            while(iter.hasNext()) {
                outputDisplayName((String)iter.next());
                
                if (iter.hasNext()) {
                	parts.add(", ");
                }
            }
        }else if(obj.isNoCache()){
            parts.add(" "); //$NON-NLS-1$
            parts.add(NOCACHE);
        }

    }

    public void visit(OrderBy obj) {
        parts.add(ORDER);
        parts.add(SPACE);
        parts.add(BY);
		parts.add(SPACE);
		for (Iterator<OrderByItem> iterator = obj.getOrderByItems().iterator(); iterator.hasNext();) {
			OrderByItem item = iterator.next();
			parts.add(registerNode(item));
			if (iterator.hasNext()) {
				parts.add( ", " ); //$NON-NLS-1$				
			}
		}
    }
    
    @Override
    public void visit(OrderByItem obj) {
    	SingleElementSymbol ses = obj.getSymbol();
	    if (ses instanceof AliasSymbol) {
	    	AliasSymbol as = (AliasSymbol)ses;
	    	outputDisplayName(as.getOutputName());
	    } else {
	    	parts.add(registerNode(ses));
	    }
        if(!obj.isAscending()) {
            parts.add(SPACE);
            parts.add(DESC);
        } // Don't print default "ASC"
        if (obj.getNullOrdering() != null) {
        	parts.add(SPACE);
        	parts.add(NonReserved.NULLS);
        	parts.add(SPACE);
        	parts.add(obj.getNullOrdering().name());
        }
    }
    
    public void visit(DynamicCommand obj) {
        parts.add(EXECUTE);
        parts.add(SPACE);
        parts.add(STRING);
        parts.add(SPACE);
        parts.add(registerNode(obj.getSql()));

        if(obj.isAsClauseSet()){
            parts.add(SPACE);
            parts.add(AS);
            parts.add(SPACE);
            for (int i = 0; i < obj.getAsColumns().size(); i++) {
                ElementSymbol symbol = (ElementSymbol)obj.getAsColumns().get(i);
                symbol.setDisplayMode(ElementSymbol.DisplayMode.SHORT_OUTPUT_NAME);
                parts.add(registerNode(symbol));
                parts.add(SPACE);
                parts.add(DataTypeManager.getDataTypeName(symbol.getType()));
                if (i < obj.getAsColumns().size() - 1) {
                    parts.add(", "); //$NON-NLS-1$
                }
            }
        }

        if(obj.getIntoGroup() != null){
            parts.add(SPACE);
            parts.add(INTO);
            parts.add(SPACE);
            parts.add(registerNode(obj.getIntoGroup()));
        }

        if(obj.getUsing() != null && !obj.getUsing().isEmpty()) {
            parts.add(SPACE);
            parts.add(USING);
            parts.add(SPACE);
            parts.add(registerNode(obj.getUsing()));
        }

        if (obj.getUpdatingModelCount() > 0) {
            parts.add(SPACE);
            parts.add(UPDATE);
            parts.add(SPACE);
            if (obj.getUpdatingModelCount() > 1) {
                parts.add("*"); //$NON-NLS-1$
            } else {
                parts.add("1"); //$NON-NLS-1$
            }
        }
    }

    public void visit(SetClauseList obj) {
    	for (Iterator<SetClause> iterator = obj.getClauses().iterator(); iterator.hasNext();) {
			SetClause clause = iterator.next();
			parts.add(registerNode(clause));
            if (iterator.hasNext()) {
                parts.add(", "); //$NON-NLS-1$
            }
		}
    }
    
    public void visit(SetClause obj) {
        ElementSymbol symbol = obj.getSymbol();
        symbol.setDisplayMode(ElementSymbol.DisplayMode.SHORT_OUTPUT_NAME);
        parts.add(registerNode(symbol));
        parts.add(" = "); //$NON-NLS-1$
        parts.add(registerNode(obj.getValue()));
    }

    public void visit(Query obj) {
    	addCacheHint(obj);
        parts.add(registerNode(obj.getSelect()));

        if(obj.getInto() != null){
            parts.add(SPACE);
            parts.add(INTO);
            parts.add(SPACE);
            parts.add(registerNode(obj.getInto()));
        }

        if(obj.getFrom() != null){
            parts.add(SPACE);
            parts.add(registerNode(obj.getFrom()));
        }

        // Where clause
        if(obj.getCriteria() != null) {
            parts.add(SPACE);
            parts.add(WHERE);
            parts.add(SPACE);
            parts.add(registerNode(obj.getCriteria()));
        }

		// Group by clause
        if(obj.getGroupBy() != null) {
            parts.add(SPACE);
            parts.add(registerNode(obj.getGroupBy()));
        }

		// Having clause
		if(obj.getHaving() != null) {
            parts.add(SPACE);
            parts.add(HAVING);
            parts.add(SPACE);
            parts.add(registerNode(obj.getHaving()));
		}

		// Order by clause
		if(obj.getOrderBy() != null) {
            parts.add(SPACE);
            parts.add(registerNode(obj.getOrderBy()));
		}
        
        if (obj.getLimit() != null) {
            parts.add(SPACE);
            parts.add(registerNode(obj.getLimit()));
        }

		// Option clause
		if(obj.getOption() != null) {
            parts.add(SPACE);
            parts.add(registerNode(obj.getOption()));
		}
    }

    public void visit(SearchedCaseExpression obj) {
        parts.add(CASE);
        for (int i = 0; i < obj.getWhenCount(); i++) {
            parts.add(SPACE);
            parts.add(WHEN);
            parts.add(SPACE);
            parts.add(registerNode(obj.getWhenCriteria(i)));
            parts.add(SPACE);
            parts.add(THEN);
            parts.add(SPACE);
            parts.add(registerNode(obj.getThenExpression(i)));
        }
        parts.add(SPACE);
        if (obj.getElseExpression() != null) {
            parts.add(ELSE);
            parts.add(SPACE);
            parts.add(registerNode(obj.getElseExpression()));
            parts.add(SPACE);
        }
        parts.add(END);
    }

    public void visit(Select obj) {
        parts.add(SELECT);
        parts.add(SPACE);

		if(obj.isDistinct()) {
			parts.add(DISTINCT);
			parts.add(SPACE);
		}

	    Iterator iter = obj.getSymbols().iterator();
        while( iter.hasNext() ) {
			SelectSymbol symbol = (SelectSymbol) iter.next();
			parts.add(registerNode(symbol));
			if(iter.hasNext()) {
				parts.add(", "); //$NON-NLS-1$
			}
        }
    }

    public void visit(SetCriteria obj) {
		// variable
		parts.add(registerNode(obj.getExpression()));

		// operator and beginning of list
		parts.add(SPACE);
        if (obj.isNegated()) {
            parts.add(NOT);
            parts.add(SPACE);
        }
		parts.add(IN);
		parts.add(" ("); //$NON-NLS-1$

		// value list
		List vals = obj.getValues();
		int size = vals.size();
		if(size == 1) {
			Iterator iter = vals.iterator();
			Expression expr = (Expression) iter.next();
			parts.add( registerNode( expr ) );
		} else if(size > 1) {
			Iterator iter = vals.iterator();
			Expression expr = (Expression) iter.next();
			parts.add( registerNode( expr ) );
			while(iter.hasNext()) {
			    expr = (Expression) iter.next();
				parts.add(", "); //$NON-NLS-1$
				parts.add( registerNode( expr ) );
			}
		}
		parts.add(")"); //$NON-NLS-1$
    }

    public void visit(SetQuery obj) {
    	addCacheHint(obj);
        QueryCommand query = obj.getLeftQuery();
        if(query instanceof Query) {
            parts.add(registerNode(query));
        } else {
            parts.add("("); //$NON-NLS-1$
            parts.add(registerNode(query));
            parts.add(")"); //$NON-NLS-1$
        }

        parts.add(SPACE);
        parts.add(obj.getOperation());
        parts.add(SPACE);

        if(obj.isAll()) {
            parts.add(ALL);
            parts.add(SPACE);
        }

        query = obj.getRightQuery();
        if(query instanceof Query) {
            parts.add(registerNode(query));
        } else {
            parts.add("("); //$NON-NLS-1$
            parts.add(registerNode(query));
            parts.add(")"); //$NON-NLS-1$
        }

        if(obj.getOrderBy() != null) {
            parts.add(SPACE);
            parts.add(registerNode(obj.getOrderBy()));
        }

        if(obj.getLimit() != null) {
            parts.add(SPACE);
            parts.add(registerNode(obj.getLimit()));
        }

        if(obj.getOption() != null) {
            parts.add(SPACE);
            parts.add(registerNode(obj.getOption()));
        }
    }

    public void visit(StoredProcedure obj) {
    	addCacheHint(obj);
        //exec clause
        parts.add(EXEC);
		parts.add(SPACE);
		parts.add(obj.getProcedureName());
		parts.add("("); //$NON-NLS-1$
		List params = obj.getInputParameters();
        if(params != null) {
            Iterator iter = params.iterator();
            while(iter.hasNext()) {
            	SPParameter param = (SPParameter) iter.next();
                
            	if (obj.displayNamedParameters()) {
            	    parts.add(escapeSinglePart(ElementSymbol.getShortName(param.getParameterSymbol().getOutputName())));
                    parts.add(" => "); //$NON-NLS-1$
                }
                
                if(param.getExpression() == null) {
                    if(param.getName() != null) {
                    	outputDisplayName(obj.getParamFullName(param));
                    } else {
                        parts.add("?"); //$NON-NLS-1$
                    }
                } else {
                	boolean addParens = !obj.displayNamedParameters() && param.getExpression() instanceof CompareCriteria;
                	if (addParens) {
                		parts.add(Tokens.LPAREN);
                	}
                    parts.add(registerNode(param.getExpression()));
                    if (addParens) {
                		parts.add(Tokens.RPAREN);
                	}
                }
                if(iter.hasNext()) {
                    parts.add(", "); //$NON-NLS-1$
                }
            }
        }
        parts.add(")"); //$NON-NLS-1$

        // Option clause
		if(obj.getOption() != null) {
            parts.add(SPACE);
            parts.add(registerNode(obj.getOption()));
		}else{
			parts.add(""); //$NON-NLS-1$
		}
    }

	private void addCacheHint(Command obj) {
		if (obj.isCache()) {
    		parts.add(BEGIN_COMMENT);
	        parts.add(SPACE);
	        parts.add(Command.CACHE);
	        parts.add(SPACE);
	        parts.add(END_COMMENT);
	        parts.add(SPACE);
    	}
	}

    public void visit(SubqueryFromClause obj) {
        addOptionComment(obj);
        if (obj.isTable()) {
        	parts.add(TABLE);
        }
        parts.add("(");//$NON-NLS-1$
        parts.add(registerNode(obj.getCommand()));
        parts.add(")");//$NON-NLS-1$
        parts.add(" AS ");//$NON-NLS-1$
        parts.add(obj.getOutputName());
        addFromClasueDepOptions(obj);
    }

    public void visit(SubquerySetCriteria obj) {
        // variable
        parts.add(registerNode(obj.getExpression()));

        // operator and beginning of list
        parts.add(SPACE);
        if (obj.isNegated()) {
            parts.add(NOT);
            parts.add(SPACE);
        }
        parts.add(IN);
        parts.add(" ("); //$NON-NLS-1$
        parts.add(registerNode(obj.getCommand()));
        parts.add(")"); //$NON-NLS-1$
    }


    public void visit(UnaryFromClause obj) {
        addOptionComment(obj);
        parts.add(registerNode(obj.getGroup()));
        addFromClasueDepOptions(obj);
    }

    public void visit(Update obj) {
        // Update clause
        parts.add(UPDATE);
		parts.add(SPACE);
        parts.add(registerNode(obj.getGroup()));
		parts.add(SPACE);

        // Set clause
        parts.add(SET);
        parts.add(SPACE);

        parts.add(registerNode(obj.getChangeList()));
        
		// Where clause
		if(obj.getCriteria() != null) {
			parts.add(SPACE);
			parts.add(WHERE);
			parts.add(SPACE);
			parts.add(registerNode(obj.getCriteria()));
		}

		// Option clause
		if(obj.getOption() != null) {
            parts.add(SPACE);
            parts.add(registerNode(obj.getOption()));
		}
    }

    public void visit(Into obj) {
        parts.add(registerNode(obj.getGroup()));
    }

    // ############ Visitor methods for symbol objects ####################

    public void visit(AggregateSymbol obj) {
        parts.add(obj.getAggregateFunction().name());
		parts.add("("); //$NON-NLS-1$

		if(obj.isDistinct()) {
			parts.add(DISTINCT);
			parts.add(" "); //$NON-NLS-1$
		}

		if(obj.getExpression() == null) {
			parts.add(Tokens.ALL_COLS);
		} else {
			parts.add(registerNode(obj.getExpression()));
		}
		
		if (obj.getOrderBy() != null) {
			parts.add(SPACE);
			parts.add(registerNode(obj.getOrderBy()));
		}
		parts.add(")"); //$NON-NLS-1$
    }

    public void visit(AliasSymbol obj) {
        parts.add(registerNode(obj.getSymbol()));
        parts.add(SPACE);
        parts.add(AS);
        parts.add(SPACE);
        parts.add(escapeSinglePart(obj.getOutputName()));
    }

    public void visit(AllInGroupSymbol obj) {
        parts.add(obj.getName());
    }

    public void visit(AllSymbol obj) {
        parts.add(obj.getName());
    }

    public void visit(Constant obj) {
        Class<?> type = obj.getType();
        Object[] constantParts = null;
        if (obj.isMultiValued()) {
        	constantParts = new Object[] {"?"}; //$NON-NLS-1$
        } else if(obj.isNull()) {
        	if(type.equals(DataTypeManager.DefaultDataClasses.BOOLEAN)) {
    			constantParts = new Object[] {UNKNOWN};
        	} else {
    			constantParts = new Object[] {"null"}; //$NON-NLS-1$
        	}
		} else {
            if(Number.class.isAssignableFrom(type)) {
                constantParts = new Object[] { obj.getValue().toString() };
            } else if(type.equals(DataTypeManager.DefaultDataClasses.BOOLEAN)) {
                constantParts = new Object[] { obj.getValue().equals(Boolean.TRUE) ? TRUE : FALSE}; 
		    } else if(type.equals(DataTypeManager.DefaultDataClasses.TIMESTAMP)) {
                constantParts = new Object[] { "{ts'", obj.getValue().toString(), "'}" }; //$NON-NLS-1$ //$NON-NLS-2$
            } else if(type.equals(DataTypeManager.DefaultDataClasses.TIME)) {
                constantParts = new Object[] { "{t'", obj.getValue().toString(), "'}" }; //$NON-NLS-1$ //$NON-NLS-2$
            } else if(type.equals(DataTypeManager.DefaultDataClasses.DATE)) {
                constantParts = new Object[] { "{d'", obj.getValue().toString(), "'}" }; //$NON-NLS-1$ //$NON-NLS-2$
            } 
            if (constantParts == null) {
            	String strValue = obj.getValue().toString();
		        strValue = escapeStringValue(strValue, "'"); //$NON-NLS-1$
			    constantParts = new Object[] { "'", strValue, "'" }; //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        replaceStringParts(constantParts);
    }

 	/**
 	 * Take a string literal and escape it as necessary.  By default, this converts ' to ''.
 	 * @param str String literal value (unquoted), never null
 	 * @return Escaped string literal value
 	 */
    static String escapeStringValue(String str, String tick) {
        return StringUtil.replaceAll(str, tick, tick + tick);
    }

    public void visit(ElementSymbol obj) {
        String name = obj.getOutputName();
        if (obj.getDisplayMode().equals(ElementSymbol.DisplayMode.FULLY_QUALIFIED)) {
            name = obj.getName();
        } else if (obj.getDisplayMode().equals(ElementSymbol.DisplayMode.SHORT_OUTPUT_NAME)) {
            String shortName = SingleElementSymbol.getShortName(name);
            //TODO: this is a hack - since we default to not supporting double quoted identifiers, we need to fully qualify reserved
            if (!isReservedWord(shortName)) {
                name = shortName;
            }
        }
        
        outputDisplayName(name);
    }

    private void outputDisplayName(String name) {
        String[] pathParts = name.split("\\."); //$NON-NLS-1$
        for (int i = 0; i < pathParts.length; i++) {
            if (i > 0) {
                parts.add(ElementSymbol.SEPARATOR);
            }
            parts.add(escapeSinglePart(pathParts[i]));
        }
    }

    public void visit(ExpressionSymbol obj) {
        parts.add(registerNode(obj.getExpression()));
    }

    public void visit(Function obj) {
        String name = obj.getName();
        Expression[] args = obj.getArgs();
		if(obj.isImplicit()) {
			// Hide this function, which is implicit
            parts.add(registerNode(args[0]));

		} else if(name.equalsIgnoreCase(CONVERT) || name.equalsIgnoreCase(CAST)) {
			parts.add(name);
			parts.add("("); //$NON-NLS-1$

			if(args != null && args.length > 0) {
				parts.add(registerNode(args[0]));

				if(name.equalsIgnoreCase(CONVERT)) {
					parts.add(", "); //$NON-NLS-1$
				} else {
					parts.add(" "); //$NON-NLS-1$
					parts.add(AS);
					parts.add(" "); //$NON-NLS-1$
				}

				if(args.length < 2 || args[1] == null || !(args[1] instanceof Constant)) {
				    parts.add(UNDEFINED);
				} else {
					parts.add(((Constant)args[1]).getValue());
				}
			}
			parts.add(")"); //$NON-NLS-1$

		} else if(name.equals("+") || name.equals("-") || name.equals("*") || name.equals("/") || name.equals("||")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			parts.add("("); //$NON-NLS-1$

			if(args != null) {
				for(int i=0; i<args.length; i++) {
					parts.add(registerNode(args[i]));
					if(i < (args.length-1)) {
						parts.add(SPACE);
						parts.add(name);
						parts.add(SPACE);
					}
				}
			}
			parts.add(")"); //$NON-NLS-1$

        } else if(name.equalsIgnoreCase(NonReserved.TIMESTAMPADD) || name.equalsIgnoreCase(NonReserved.TIMESTAMPDIFF)) {
            parts.add(name);
            parts.add("("); //$NON-NLS-1$

            if(args != null && args.length > 0) {
                parts.add(((Constant)args[0]).getValue());
                registerNodes(args, 1);
            }
            parts.add(")"); //$NON-NLS-1$

		} else if (name.equalsIgnoreCase(SourceSystemFunctions.XMLPI)){
			parts.add(name);
			parts.add("(NAME "); //$NON-NLS-1$
			outputDisplayName((String)((Constant)args[0]).getValue());
			registerNodes(args, 1);
			parts.add(")"); //$NON-NLS-1$
		} else {
			parts.add(name);
			parts.add("("); //$NON-NLS-1$
			registerNodes(args, 0);
			parts.add(")"); //$NON-NLS-1$
		}
    }
    
    private void registerNodes(LanguageObject[] objects, int begin) {
    	registerNodes(Arrays.asList(objects), begin);
    }
    
    private void registerNodes(List<? extends LanguageObject> objects, int begin) {
    	for (int i = begin; i < objects.size(); i++) {
    		if (i > 0) {
    			parts.add(", "); //$NON-NLS-1$
    		}
			parts.add(registerNode(objects.get(i)));
		}
    }

    public void visit(GroupSymbol obj) {
        String alias = null;
        String fullGroup = obj.getOutputName();
        if(obj.getOutputDefinition() != null) {
            alias = obj.getOutputName();
            fullGroup = obj.getOutputDefinition();
        }
        
        outputDisplayName(fullGroup);

        if(alias != null) {
            parts.add(SPACE);
            parts.add(AS);
            parts.add(SPACE);
            parts.add(escapeSinglePart(alias));
        }
    }

    public void visit(Reference obj) {
        if (!obj.isPositional() && obj.getExpression() != null) {
            replaceStringParts(new Object[] { obj.getExpression().toString() });
        } else {
            replaceStringParts(new Object[] { "?" }); //$NON-NLS-1$
        }
    }

    // ############ Visitor methods for storedprocedure language objects ####################

    public void visit(Block obj) {
    	List statements = obj.getStatements();
    	if(statements.size() == 1) {
    		replaceStringParts(new Object[] { BEGIN, "\n", //$NON-NLS-1$
			registerNode((Statement)obj.getStatements().get(0)), "\n", END}); //$NON-NLS-1$
    	} else if(statements.size() > 1) {
	        List parts = new ArrayList();
            // Add first clause
            parts.add(BEGIN);
            parts.add("\n"); //$NON-NLS-1$
            Iterator stmtIter = statements.iterator();
            while(stmtIter.hasNext()) {
				// Add each statement
	            parts.add(registerNode((Statement) stmtIter.next()));
                parts.add("\n"); //$NON-NLS-1$
            }
            parts.add(END);
            replaceStringParts(parts.toArray());
        } else {
            // Shouldn't happen, but being tolerant
            replaceStringParts(new Object[] { BEGIN, "\n", //$NON-NLS-1$
            							END });
        }
    }

    public void visit(CommandStatement obj) {
        parts.add(registerNode(obj.getCommand()));
		parts.add(";"); //$NON-NLS-1$
    }

    public void visit(CreateUpdateProcedureCommand obj) {
        parts.add(CREATE);
        parts.add(SPACE);
        if(!obj.isUpdateProcedure()){
            parts.add(VIRTUAL);
            parts.add(SPACE);
        }
        parts.add(PROCEDURE);
        parts.add("\n"); //$NON-NLS-1$
        parts.add(registerNode(obj.getBlock()));
    }

    public void visit(DeclareStatement obj) {
		parts.add(DECLARE);
		parts.add(SPACE);
        parts.add(obj.getVariableType());
        parts.add(SPACE);
        createAssignment(obj);
    }

    /** 
     * @param obj
     * @param parts
     */
    private void createAssignment(AssignmentStatement obj) {
        parts.add(registerNode(obj.getVariable()));
        if (obj.getValue() != null) {
            parts.add(" = "); //$NON-NLS-1$
            parts.add(registerNode(obj.getValue()));
        }
		parts.add(";"); //$NON-NLS-1$
    }

    public void visit(IfStatement obj) {
        parts.add(IF);
        parts.add("("); //$NON-NLS-1$
        parts.add(registerNode(obj.getCondition()));
        parts.add(")\n"); //$NON-NLS-1$
        parts.add(registerNode(obj.getIfBlock()));
        if(obj.hasElseBlock()) {
        	parts.add("\n"); //$NON-NLS-1$
	        parts.add(ELSE);
	        parts.add("\n"); //$NON-NLS-1$
	        parts.add(registerNode(obj.getElseBlock()));
        }
    }

    public void visit(AssignmentStatement obj) {
        createAssignment(obj);
    }

    public void visit(HasCriteria obj) {
        parts.add( HAS);
        parts.add(SPACE);
        parts.add(registerNode(obj.getSelector()));
    }

    public void visit(TranslateCriteria obj) {
        parts.add(TRANSLATE);
        parts.add(SPACE);
        parts.add(registerNode(obj.getSelector()));

        if(obj.hasTranslations()) {
	        parts.add(SPACE);
	        parts.add(WITH);
	        parts.add(SPACE);
        	parts.add("("); //$NON-NLS-1$
	        Iterator critIter = obj.getTranslations().iterator();

	        while(critIter.hasNext()) {
				parts.add(registerNode((Criteria)critIter.next()));
				if(critIter.hasNext()) {
					parts.add(", "); //$NON-NLS-1$
				}
				if(!critIter.hasNext()) {
		        	parts.add(")"); //$NON-NLS-1$
				}
	        }
        }
    }

    public void visit(CriteriaSelector obj) {
        int selectorType = obj.getSelectorType();

        switch(selectorType) {
        	case CriteriaSelector.COMPARE_EQ:
        		parts.add("= "); //$NON-NLS-1$
        		break;
        	case CriteriaSelector.COMPARE_GE:
        		parts.add(">= "); //$NON-NLS-1$
        		break;
        	case CriteriaSelector.COMPARE_GT:
        		parts.add("> "); //$NON-NLS-1$
        		break;
        	case CriteriaSelector.COMPARE_LE:
        		parts.add("<= "); //$NON-NLS-1$
        		break;
        	case CriteriaSelector.COMPARE_LT:
        		parts.add("< "); //$NON-NLS-1$
        		break;
        	case CriteriaSelector.COMPARE_NE:
        		parts.add("<> "); //$NON-NLS-1$
        		break;
        	case CriteriaSelector.IN:
        		parts.add(IN);
        		parts.add(SPACE);
        		break;
        	case CriteriaSelector.IS_NULL:
        		parts.add(IS);
        		parts.add(SPACE);
        		parts.add(NULL);
        		parts.add(SPACE);
        		break;
            case CriteriaSelector.LIKE:
                parts.add(LIKE);
                parts.add(SPACE);
                break;
            case CriteriaSelector.BETWEEN:
                parts.add(BETWEEN);
                parts.add(SPACE);
                break;
        }

        parts.add(CRITERIA);
		if(obj.hasElements()) {
	        parts.add(SPACE);
	        parts.add(ON);
	        parts.add(SPACE);
	        parts.add("("); //$NON-NLS-1$

	        Iterator elmtIter = obj.getElements().iterator();
	        while(elmtIter.hasNext()) {
				parts.add(registerNode((ElementSymbol)elmtIter.next()));
				if(elmtIter.hasNext()) {
					parts.add(", "); //$NON-NLS-1$
				}
	        }
	        parts.add(")"); //$NON-NLS-1$
		}
    }

    public void visit(RaiseErrorStatement obj) {
        Object parts[] = new Object[4];

        parts[0] = ERROR;
        parts[1] = SPACE;
        parts[2] = registerNode(obj.getExpression());
        parts[3] = ";"; //$NON-NLS-1$
        replaceStringParts(parts);
    }

    public void visit(BreakStatement obj) {
        parts.add(BREAK);
        parts.add(";"); //$NON-NLS-1$
    }

    public void visit(ContinueStatement obj) {
        parts.add(CONTINUE);
        parts.add(";"); //$NON-NLS-1$
    }

    public void visit(LoopStatement obj) {
        parts.add(LOOP);
        parts.add(" "); //$NON-NLS-1$
        parts.add(ON);
        parts.add(" ("); //$NON-NLS-1$
        parts.add(registerNode(obj.getCommand()));
        parts.add(") "); //$NON-NLS-1$
        parts.add(AS);
        parts.add(" "); //$NON-NLS-1$
        parts.add(obj.getCursorName());
        parts.add("\n"); //$NON-NLS-1$
        parts.add(registerNode(obj.getBlock()));
    }

    public void visit(WhileStatement obj) {
        parts.add(WHILE);
        parts.add("("); //$NON-NLS-1$
        parts.add(registerNode(obj.getCondition()));
        parts.add(")\n"); //$NON-NLS-1$
        parts.add(registerNode(obj.getBlock()));
    }

    public void visit(ExistsCriteria obj) {
        // operator and beginning of list
        parts.add(EXISTS);
        parts.add(" ("); //$NON-NLS-1$
        parts.add(registerNode(obj.getCommand()));
        parts.add(")"); //$NON-NLS-1$
    }

    public void visit(SubqueryCompareCriteria obj){
        Expression leftExpression = obj.getLeftExpression();
        parts.add(registerNode(leftExpression));

        String operator = obj.getOperatorAsString();
        String quantifier = obj.getPredicateQuantifierAsString();

        // operator and beginning of list
        parts.add(SPACE);
        parts.add(operator);
        parts.add(SPACE);
        parts.add(quantifier);
        parts.add("("); //$NON-NLS-1$
        parts.add(registerNode(obj.getCommand()));
        parts.add(")"); //$NON-NLS-1$
    }

    public void visit(ScalarSubquery obj) {
        // operator and beginning of list
        parts.add("("); //$NON-NLS-1$
        parts.add(registerNode(obj.getCommand()));
        parts.add(")"); //$NON-NLS-1$
    }
    
    @Override
    public void visit(XMLAttributes obj) {
    	parts.add(XMLATTRIBUTES);
    	parts.add("("); //$NON-NLS-1$
    	registerNodes(obj.getArgs(), 0);
    	parts.add(")"); //$NON-NLS-1$
    }
    
    @Override
    public void visit(XMLElement obj) {
    	parts.add(XMLELEMENT);
    	parts.add("(NAME "); //$NON-NLS-1$
    	outputDisplayName(obj.getName());
    	if (obj.getNamespaces() != null) {
    		parts.add(", "); //$NON-NLS-1$
    		parts.add(registerNode(obj.getNamespaces()));
    	}
    	if (obj.getAttributes() != null) {
    		parts.add(", "); //$NON-NLS-1$
    		parts.add(registerNode(obj.getAttributes()));
    	}
    	if (!obj.getContent().isEmpty()) {
    		parts.add(", "); //$NON-NLS-1$
    	}
		registerNodes(obj.getContent(), 0);
    	parts.add(")"); //$NON-NLS-1$
    }
    
    @Override
    public void visit(XMLForest obj) {
    	parts.add(XMLFOREST);
    	parts.add("("); //$NON-NLS-1$
    	if (obj.getNamespaces() != null) {
    		parts.add(registerNode(obj.getNamespaces()));
    		parts.add(", "); //$NON-NLS-1$
    	}
    	registerNodes(obj.getArgs(), 0);
    	parts.add(")"); //$NON-NLS-1$
    }
    
    @Override
    public void visit(XMLNamespaces obj) {
    	parts.add(XMLNAMESPACES);
    	parts.add("("); //$NON-NLS-1$
    	for (Iterator<NamespaceItem> items = obj.getNamespaceItems().iterator(); items.hasNext();) {
    		NamespaceItem item = items.next();
    		if (item.getPrefix() == null) {
    			if (item.getUri() == null) {
    				parts.add("NO DEFAULT"); //$NON-NLS-1$
    			} else {
	    			parts.add("DEFAULT "); //$NON-NLS-1$
	    			parts.add(registerNode(new Constant(item.getUri())));
    			}
    		} else {
    			parts.add(registerNode(new Constant(item.getUri())));
    			parts.add(" AS "); //$NON-NLS-1$
        		outputDisplayName(item.getPrefix());
    		}
    		if (items.hasNext()) {
    			parts.add(", "); //$NON-NLS-1$
    		}
    	}
    	parts.add(")"); //$NON-NLS-1$
    }
    
    public void visit(Limit obj) {
        parts.add(LIMIT);
        if (obj.getOffset() != null) {
            parts.add(SPACE);
            parts.add(registerNode(obj.getOffset()));
            parts.add(","); //$NON-NLS-1$
        }
        parts.add(SPACE);
        parts.add(registerNode(obj.getRowLimit()));
    }
    
    @Override
    public void visit(TextTable obj) {
    	parts.add("TEXTTABLE("); //$NON-NLS-1$
    	parts.add(registerNode(obj.getFile()));
    	parts.add(SPACE);
    	parts.add(NonReserved.COLUMNS);
    	
    	for (Iterator<TextColumn> cols = obj.getColumns().iterator(); cols.hasNext();) {
    		TextColumn col = cols.next();
    		parts.add(SPACE);
    		outputDisplayName(col.getName());
    		parts.add(SPACE);
    		parts.add(col.getType());
    		if (col.getWidth() != null) {
        		parts.add(SPACE);
    			parts.add(NonReserved.WIDTH);
            	parts.add(SPACE);
    			parts.add(col.getWidth());
    		}
    		if (cols.hasNext()) {
    			parts.add(","); //$NON-NLS-1$
    		}
		}
    	if (obj.getDelimiter() != null) {
        	parts.add(SPACE);
    		parts.add(NonReserved.DELIMITER);
        	parts.add(SPACE);
    		parts.add(registerNode(new Constant(obj.getDelimiter())));
    	}
    	if (obj.getQuote() != null) {
        	parts.add(SPACE);
        	if (obj.isEscape()) {
        		parts.add(ESCAPE);
        	} else {
        		parts.add(NonReserved.QUOTE);
        	}
        	parts.add(SPACE);
    		parts.add(registerNode(new Constant(obj.getQuote())));
    	}
    	if (obj.getHeader() != null) {
        	parts.add(SPACE);
    		parts.add(NonReserved.HEADER);
    		if (1 != obj.getHeader()) {
	        	parts.add(SPACE);
	    		parts.add(obj.getHeader());
    		}
    	}
    	if (obj.getSkip() != null) {
        	parts.add(SPACE);
    		parts.add("SKIP"); //$NON-NLS-1$
        	parts.add(SPACE);
    		parts.add(obj.getSkip());
    	}
    	parts.add(")");//$NON-NLS-1$
    	parts.add(SPACE);
    	parts.add(AS);
    	parts.add(SPACE);
		outputDisplayName(obj.getName());
    }

    @Override
    public void visit(XMLTable obj) {
    	parts.add("XMLTABLE("); //$NON-NLS-1$
    	if (obj.getNamespaces() != null) {
    		parts.add(registerNode(obj.getNamespaces()));
    		parts.add(","); //$NON-NLS-1$
    		parts.add(SPACE);
    	}
    	parts.add(new Constant(obj.getXquery()));
    	if (!obj.getPassing().isEmpty()) {
    		parts.add(SPACE);
        	parts.add(NonReserved.PASSING);
        	parts.add(SPACE);
	    	registerNodes(obj.getPassing(), 0);
    	}
    	if (!obj.getColumns().isEmpty()) {
    		parts.add(SPACE);
        	parts.add(NonReserved.COLUMNS);
	    	for (Iterator<XMLColumn> cols = obj.getColumns().iterator(); cols.hasNext();) {
	    		XMLColumn col = cols.next();
	    		parts.add(SPACE);
	    		outputDisplayName(col.getName());
	    		parts.add(SPACE);
	    		if (col.isOrdinal()) {
	    			parts.add(FOR);
	    			parts.add(SPACE);
	    			parts.add(NonReserved.ORDINALITY); 
	    		} else {
	    			parts.add(col.getType());
		    		if (col.getDefaultExpression() != null) {
		        		parts.add(SPACE);
		    			parts.add(DEFAULT);
		            	parts.add(SPACE);
		    			parts.add(registerNode(col.getDefaultExpression()));
		    		}
		    		if (col.getPath() != null) {
		        		parts.add(SPACE);
		    			parts.add(NonReserved.PATH); 
		            	parts.add(SPACE);
		    			parts.add(new Constant(col.getPath()));
		    		}
	    		}
	    		if (cols.hasNext()) {
	    			parts.add(","); //$NON-NLS-1$
	    		}
			}
    	}
    	parts.add(")");//$NON-NLS-1$
    	parts.add(SPACE);
    	parts.add(AS);
    	parts.add(SPACE);
		outputDisplayName(obj.getName());
    }
    
    @Override
    public void visit(XMLQuery obj) {
    	parts.add("XMLQUERY("); //$NON-NLS-1$
    	if (obj.getNamespaces() != null) {
    		parts.add(registerNode(obj.getNamespaces()));
    		parts.add(","); //$NON-NLS-1$
    		parts.add(SPACE);
    	}
    	parts.add(new Constant(obj.getXquery()));
    	if (!obj.getPassing().isEmpty()) {
    		parts.add(SPACE);
        	parts.add(NonReserved.PASSING);
        	parts.add(SPACE);
	    	registerNodes(obj.getPassing(), 0);
    	}
    	if (obj.getEmptyOnEmpty() != null) {
    		parts.add(SPACE);
    		if (obj.getEmptyOnEmpty()) {
        		parts.add(NonReserved.EMPTY);    			
    		} else {
    			parts.add(NULL);
    		}
    		parts.add(SPACE);
    		parts.add(ON);
    		parts.add(SPACE);
    		parts.add(NonReserved.EMPTY);
    	}
    	parts.add(")");//$NON-NLS-1$
    }
    
    @Override
    public void visit(DerivedColumn obj) {
    	parts.add(registerNode(obj.getExpression()));
    	if (obj.getAlias() != null) {
    		parts.add(SPACE);
    		parts.add(AS);
    		parts.add(SPACE);
    		outputDisplayName(obj.getAlias());
    	}
    }
    
    @Override
    public void visit(XMLSerialize obj) {
    	parts.add(XMLSERIALIZE);
    	parts.add(Tokens.LPAREN);
    	if (obj.isDocument() != null) {
	    	if (obj.isDocument()) {
	    		parts.add(NonReserved.DOCUMENT);
	    	} else {
	    		parts.add(NonReserved.CONTENT);
	    	}
	    	parts.add(SPACE);
    	}
    	parts.add(registerNode(obj.getExpression()));
    	if (obj.getTypeString() != null) {
    		parts.add(SPACE);
        	parts.add(AS);
        	parts.add(SPACE);
        	parts.add(obj.getTypeString());
    	}
    	parts.add(Tokens.RPAREN);
    }
    
    @Override
    public void visit(QueryString obj) {
    	parts.add(NonReserved.QUERYSTRING);
    	parts.add("("); //$NON-NLS-1$
    	parts.add(registerNode(obj.getPath()));
    	if (!obj.getArgs().isEmpty()) {
    		parts.add(","); //$NON-NLS-1$
	    	parts.add(SPACE);
	    	registerNodes(obj.getArgs(), 0);
    	}
    	parts.add(")"); //$NON-NLS-1$
    }
    
    @Override
    public void visit(XMLParse obj) {
    	parts.add(XMLPARSE);
    	parts.add(Tokens.LPAREN);
    	if (obj.isDocument()) {
    		parts.add(NonReserved.DOCUMENT);
    	} else {
    		parts.add(NonReserved.CONTENT);
    	}
    	parts.add(SPACE);
    	parts.add(registerNode(obj.getExpression()));
    	if (obj.isWellFormed()) {
    		parts.add(SPACE);
    		parts.add(NonReserved.WELLFORMED);
    	}
    	parts.add(Tokens.RPAREN);
    }
    
    @Override
    public void visit(ExpressionCriteria obj) {
    	obj.getExpression().acceptVisitor(this);
    }

    public static String escapeSinglePart(String part) {
    	if(isReservedWord(part)) {
    	    return ID_ESCAPE_CHAR + part + ID_ESCAPE_CHAR;
    	}
    	boolean escape = true;
    	char start = part.charAt(0);
    	if (start == '#' || start == '@' || StringUtil.isLetter(start)) {
    		escape = false;
    		for (int i = 1; !escape && i < part.length(); i++) {
    			char c = part.charAt(i);
    			escape = !StringUtil.isLetterOrDigit(c) && c != '_';
    		}
    	}
    	if (escape) {
    		return ID_ESCAPE_CHAR + escapeStringValue(part, "\"") + ID_ESCAPE_CHAR; //$NON-NLS-1$
    	}
    	return part;
    }

    /**
     * Check whether a string is considered a reserved word or not.  Subclasses
     * may override to change definition of reserved word.
     * @param string String to check
     * @return True if reserved word
     */
    static boolean isReservedWord(String string) {
    	if(string == null) {
    	    return false;
    	}
   		return SQLConstants.isReservedWord(string);
    }
    
}
