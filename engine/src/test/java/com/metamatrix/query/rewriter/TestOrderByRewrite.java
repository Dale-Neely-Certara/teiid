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

package com.metamatrix.query.rewriter;

import static com.metamatrix.query.rewriter.TestQueryRewriter.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import com.metamatrix.api.exception.MetaMatrixComponentException;
import com.metamatrix.api.exception.MetaMatrixProcessingException;
import com.metamatrix.query.parser.QueryParser;
import com.metamatrix.query.resolver.QueryResolver;
import com.metamatrix.query.sql.LanguageObject;
import com.metamatrix.query.sql.lang.Command;
import com.metamatrix.query.sql.lang.OrderBy;
import com.metamatrix.query.sql.lang.Query;
import com.metamatrix.query.sql.symbol.ElementSymbol;
import com.metamatrix.query.sql.symbol.ExpressionSymbol;
import com.metamatrix.query.sql.symbol.SingleElementSymbol;
import com.metamatrix.query.sql.visitor.ElementCollectorVisitor;
import com.metamatrix.query.unittest.FakeMetadataFactory;
import com.metamatrix.query.unittest.FakeMetadataObject;

/**
 * Converted from older resolver tests
 */
public class TestOrderByRewrite  {
    
    private static Command getCommand(String sql) throws MetaMatrixComponentException, MetaMatrixProcessingException {
        Command command = QueryParser.getQueryParser().parseCommand(sql);
        
        QueryResolver.resolveCommand(command, FakeMetadataFactory.example1Cached());
        
        return QueryRewriter.rewrite(command, FakeMetadataFactory.example1Cached(), null);
    }

    private void helpCheckElements(OrderBy langObj,
                                   String[] elementNames,
                                   String[] elementIDs) {
        List elements = new ArrayList();
        for (Iterator i = langObj.getSortKeys().iterator(); i.hasNext();) {
            ElementCollectorVisitor.getElements((LanguageObject)i.next(), elements);
        }

        assertEquals("Wrong number of elements: ", elementNames.length, elements.size()); //$NON-NLS-1$

        for (int i = 0; i < elements.size(); i++) {
            ElementSymbol symbol = (ElementSymbol)elements.get(i);
            assertEquals("Element name does not match: ", elementNames[i].toUpperCase(), symbol.getName().toUpperCase()); //$NON-NLS-1$

            FakeMetadataObject elementID = (FakeMetadataObject)symbol.getMetadataID();
            assertNotNull("ElementSymbol " + symbol + " was not resolved and has no metadataID", elementID); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("ElementID name does not match: ", elementIDs[i].toUpperCase(), elementID.getName().toUpperCase()); //$NON-NLS-1$
        }
    }
    
    private void helpCheckExpressionsSymbols(OrderBy langObj,
                                             String[] functionsNames) {
    	int expCount = 0;
        for (Iterator i = langObj.getSortKeys().iterator(); i.hasNext();) {
        	SingleElementSymbol ses = (SingleElementSymbol)i.next();
            if (ses instanceof ExpressionSymbol) {
                assertEquals("Expression Symbols does not match: ", functionsNames[expCount++], ses.toString()); //$NON-NLS-1$                        		
            }
        }
        assertEquals("Wrong number of Symbols: ", functionsNames.length, expCount); //$NON-NLS-1$
    }
    
    @Test public void testNumberedOrderBy1() throws Exception {
        Query resolvedQuery = (Query) getCommand("SELECT pm1.g1.e1, e2, e3 as x, (5+2) as y FROM pm1.g1 ORDER BY 3, 4, 1, 2"); //$NON-NLS-1$
        helpCheckElements(resolvedQuery.getOrderBy(), 
            new String[] { "pm1.g1.e3", "pm1.g1.e1", "pm1.g1.e2" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            new String[] { "pm1.g1.e3", "pm1.g1.e1", "pm1.g1.e2" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        
        helpCheckExpressionsSymbols(resolvedQuery.getOrderBy(),
            new String[] {});
    }

    @Test public void testNumberedOrderBy1_1() throws Exception {
        Query resolvedQuery = (Query) getCommand("SELECT pm1.g1.e1, e2, e3 as x, (5 + e4) FROM pm1.g1 ORDER BY 3, 4, 1, 2"); //$NON-NLS-1$
        helpCheckElements(resolvedQuery.getOrderBy(), 
            new String[] { "pm1.g1.e3", "pm1.g1.e4", "pm1.g1.e1", "pm1.g1.e2" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            new String[] { "pm1.g1.e3", "pm1.g1.e4", "pm1.g1.e1", "pm1.g1.e2" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        
        helpCheckExpressionsSymbols(resolvedQuery.getOrderBy(),
            new String[] {"(5.0 + e4)"}); //$NON-NLS-1$
    }
    
    @Test public void testNumberedOrderBy1_2() throws Exception {
        Query resolvedQuery = (Query) getCommand("SELECT pm1.g1.e1, e2, concat(e3,'x'), concat(e2, 5) FROM pm1.g1 ORDER BY 3, 4, 1, 2"); //$NON-NLS-1$
        helpCheckElements(resolvedQuery.getOrderBy(), 
            new String[] { "pm1.g1.e3", "pm1.g1.e2", "pm1.g1.e1", "pm1.g1.e2" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            new String[] { "pm1.g1.e3", "pm1.g1.e2", "pm1.g1.e1", "pm1.g1.e2" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        
        helpCheckExpressionsSymbols(resolvedQuery.getOrderBy(),
            new String[] {"concat(e3, 'x')", "concat(e2, '5')"}); //$NON-NLS-1$ //$NON-NLS-2$
    }
 
    @Test public void testNumberedOrderBy1_3() throws Exception {
        Query resolvedQuery = (Query) getCommand("SELECT pm1.g1.e1, avg(e2), e3, concat(e2, 5) FROM pm1.g1 ORDER BY 3, 4, 1, 2"); //$NON-NLS-1$
        helpCheckElements(resolvedQuery.getOrderBy(), 
            new String[] { "pm1.g1.e3", "pm1.g1.e2", "pm1.g1.e1", "pm1.g1.e2" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            new String[] { "pm1.g1.e3", "pm1.g1.e2", "pm1.g1.e1", "pm1.g1.e2" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        
        helpCheckExpressionsSymbols(resolvedQuery.getOrderBy(),
            new String[] {"concat(e2, '5')", "AVG(e2)"}); //$NON-NLS-1$ //$NON-NLS-2$
    }
    
    @Test public void testNumberedOrderBy1_4() throws Exception {
        String sql = "select e1, (select e2 from pm4.g1) from pm4.g2 X order by 2"; //$NON-NLS-1$
        Query resolvedQuery = (Query) getCommand(sql); 
        
        helpCheckExpressionsSymbols(resolvedQuery.getOrderBy(),
                new String[] {"(SELECT e2 FROM pm4.g1)"}); //$NON-NLS-1$        
    }
    
    @Test public void testOrderBy1() throws Exception {
        Query resolvedQuery = (Query) getCommand("SELECT pm1.g1.e1, e2, e3 as x, (5+2) as y FROM pm1.g1 ORDER BY x, y, pm1.g1.e1, e2"); //$NON-NLS-1$
        helpCheckElements(resolvedQuery.getOrderBy(), 
            new String[] { "pm1.g1.e3", "pm1.g1.e1", "pm1.g1.e2" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            new String[] { "pm1.g1.e3", "pm1.g1.e1", "pm1.g1.e2" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    @Test public void testOrderBy2() throws Exception {
        Query resolvedQuery = (Query) getCommand("SELECT * FROM pm1.g1 ORDER BY e1"); //$NON-NLS-1$
        helpCheckElements(resolvedQuery.getOrderBy(), 
            new String[] { "pm1.g1.e1" }, //$NON-NLS-1$
            new String[] { "pm1.g1.e1" }); //$NON-NLS-1$
    }

    @Test public void testOrderBy3() throws Exception {
        Query resolvedQuery = (Query) getCommand("SELECT * FROM pm1.g1 ORDER BY pm1.g1.e1"); //$NON-NLS-1$
        helpCheckElements(resolvedQuery.getOrderBy(), 
            new String[] { "pm1.g1.e1" }, //$NON-NLS-1$
            new String[] { "pm1.g1.e1" }); //$NON-NLS-1$
    }

    @Test public void testOrderBy4() throws Exception {
        Query resolvedQuery = (Query) getCommand("SELECT e1 FROM pm1.g1 ORDER BY pm1.g1.e1"); //$NON-NLS-1$
        helpCheckElements(resolvedQuery.getOrderBy(), 
            new String[] { "pm1.g1.e1" }, //$NON-NLS-1$
            new String[] { "pm1.g1.e1" }); //$NON-NLS-1$
    }

    @Test public void testOrderBy5() throws Exception {
        Query resolvedQuery = (Query) getCommand("SELECT e1 FROM pm1.g1 ORDER BY e1"); //$NON-NLS-1$
        helpCheckElements(resolvedQuery.getOrderBy(), 
            new String[] { "pm1.g1.e1" }, //$NON-NLS-1$
            new String[] { "pm1.g1.e1" }); //$NON-NLS-1$
    }

    @Test public void testOrderBy6() throws Exception {
        Query resolvedQuery = (Query) getCommand("SELECT e1 FROM pm1.g1 AS x ORDER BY e1"); //$NON-NLS-1$
        helpCheckElements(resolvedQuery.getOrderBy(), 
            new String[] { "x.e1" }, //$NON-NLS-1$
            new String[] { "pm1.g1.e1" }); //$NON-NLS-1$
    }

    @Test public void testOrderBy7() throws Exception {
        Query resolvedQuery = (Query) getCommand("SELECT e1 FROM pm1.g1 AS x ORDER BY x.e1"); //$NON-NLS-1$
        helpCheckElements(resolvedQuery.getOrderBy(), 
            new String[] { "x.e1" }, //$NON-NLS-1$
            new String[] { "pm1.g1.e1" }); //$NON-NLS-1$
    }

    @Test public void testOrderBy8() throws Exception {
        Query resolvedQuery = (Query) getCommand("SELECT x.e1 FROM pm1.g1 AS x ORDER BY e1"); //$NON-NLS-1$
        helpCheckElements(resolvedQuery.getOrderBy(), 
            new String[] { "x.e1" }, //$NON-NLS-1$
            new String[] { "pm1.g1.e1" }); //$NON-NLS-1$
    }

    @Test public void testOrderBy9() throws Exception {
        Query resolvedQuery = (Query) getCommand("SELECT x.e1 FROM pm1.g1 AS x ORDER BY x.e1"); //$NON-NLS-1$
        helpCheckElements(resolvedQuery.getOrderBy(), 
            new String[] { "x.e1" }, //$NON-NLS-1$
            new String[] { "pm1.g1.e1" }); //$NON-NLS-1$
    }

    @Test public void testOrderBy10() throws Exception {
        Query resolvedQuery = (Query) getCommand("SELECT a.e1, b.e1 FROM pm1.g1 AS a, pm1.g1 AS b ORDER BY a.e1"); //$NON-NLS-1$
        helpCheckElements(resolvedQuery.getOrderBy(), 
            new String[] { "a.e1" }, //$NON-NLS-1$
            new String[] { "pm1.g1.e1" }); //$NON-NLS-1$
    }
    
    @Test public void testOrderBy11() throws Exception {
        Query resolvedQuery = (Query) getCommand("SELECT a.e1, b.e1 FROM pm1.g1 AS a, pm1.g1 AS b ORDER BY b.e1"); //$NON-NLS-1$
        helpCheckElements(resolvedQuery.getOrderBy(), 
            new String[] { "b.e1" }, //$NON-NLS-1$
            new String[] { "pm1.g1.e1" }); //$NON-NLS-1$
    }

    @Test public void testOrderBy12() throws Exception {
        Query resolvedQuery = (Query) getCommand("SELECT a.e1, pm1.g1.e1 FROM pm1.g1 AS a, pm1.g1 ORDER BY a.e1"); //$NON-NLS-1$
        helpCheckElements(resolvedQuery.getOrderBy(), 
            new String[] { "a.e1" }, //$NON-NLS-1$
            new String[] { "pm1.g1.e1" }); //$NON-NLS-1$
    }
    
    @Test public void testOrderBy13() throws Exception {
        Query resolvedQuery = (Query) getCommand("SELECT a.e1, pm1.g1.e1 FROM pm1.g1 AS a, pm1.g1 ORDER BY pm1.g1.e1"); //$NON-NLS-1$
        helpCheckElements(resolvedQuery.getOrderBy(), 
            new String[] { "pm1.g1.e1" }, //$NON-NLS-1$
            new String[] { "pm1.g1.e1" }); //$NON-NLS-1$
    }

    @Test public void testOrderBy14() throws Exception {
        Query resolvedQuery = (Query) getCommand("SELECT a.e1 as x, pm1.g1.e1 as y FROM pm1.g1 AS a, pm1.g1 ORDER BY x"); //$NON-NLS-1$
        helpCheckElements(resolvedQuery.getOrderBy(), 
            new String[] { "a.e1" }, //$NON-NLS-1$
            new String[] { "pm1.g1.e1" }); //$NON-NLS-1$
    }
    
    @Test public void testOrderBy15() throws Exception {
        Query resolvedQuery = (Query) getCommand("SELECT a.e1 as x, pm1.g1.e1 as y FROM pm1.g1 AS a, pm1.g1 ORDER BY y"); //$NON-NLS-1$
        helpCheckElements(resolvedQuery.getOrderBy(), 
            new String[] { "pm1.g1.e1" }, //$NON-NLS-1$
            new String[] { "pm1.g1.e1" }); //$NON-NLS-1$
    }
    
    @Test public void testNumberedOrderBy2() throws Exception {
        Query resolvedQuery = (Query) getCommand("SELECT * FROM pm1.g1 ORDER BY 1"); //$NON-NLS-1$
        helpCheckElements(resolvedQuery.getOrderBy(), 
            new String[] { "pm1.g1.e1" }, //$NON-NLS-1$
            new String[] { "pm1.g1.e1" }); //$NON-NLS-1$
    }

    @Test public void testNumberedOrderBy3() throws Exception {
        Query resolvedQuery = (Query) getCommand("SELECT * FROM pm1.g1 ORDER BY 1"); //$NON-NLS-1$
        helpCheckElements(resolvedQuery.getOrderBy(), 
            new String[] { "pm1.g1.e1" }, //$NON-NLS-1$
            new String[] { "pm1.g1.e1" }); //$NON-NLS-1$
    }

    @Test public void testNumberedOrderBy4() throws Exception {
        Query resolvedQuery = (Query) getCommand("SELECT e1 FROM pm1.g1 ORDER BY 1"); //$NON-NLS-1$
        helpCheckElements(resolvedQuery.getOrderBy(), 
            new String[] { "pm1.g1.e1" }, //$NON-NLS-1$
            new String[] { "pm1.g1.e1" }); //$NON-NLS-1$
    }

    @Test public void testNumberedOrderBy5() throws Exception {
        Query resolvedQuery = (Query) getCommand("SELECT x.e1 FROM pm1.g1 AS x ORDER BY 1"); //$NON-NLS-1$
        helpCheckElements(resolvedQuery.getOrderBy(), 
            new String[] { "x.e1" }, //$NON-NLS-1$
            new String[] { "pm1.g1.e1" }); //$NON-NLS-1$
    }

    @Test public void testNumberedOrderBy8() throws Exception {
        Query resolvedQuery = (Query) getCommand("SELECT a.e1 as x, pm1.g1.e1 as y FROM pm1.g1 AS a, pm1.g1 ORDER BY 1"); //$NON-NLS-1$
        helpCheckElements(resolvedQuery.getOrderBy(), 
            new String[] { "a.e1" }, //$NON-NLS-1$
            new String[] { "pm1.g1.e1" }); //$NON-NLS-1$
    }
    
    /**
     * partially-qualified ORDER BY's with ambiguous short group names
     */
    @Test public void testDefect10729() throws Exception {
        Query resolvedQuery = (Query) getCommand("SELECT pm1.g1.e1 FROM pm1.g1 ORDER BY g1.e1"); //$NON-NLS-1$
        helpCheckElements(resolvedQuery.getOrderBy(), 
            new String[] { "pm1.g1.e1" }, //$NON-NLS-1$ 
            new String[] { "pm1.g1.e1" } ); //$NON-NLS-1$ 
    }                

    /**
     * partially-qualified ORDER BY's with ambiguous short group names
     */
    @Test public void testDefect10729a() throws Exception {
        Query resolvedQuery = (Query) getCommand("SELECT pm1.g1.e1 FROM pm1.g1 ORDER BY e1"); //$NON-NLS-1$
        helpCheckElements(resolvedQuery.getOrderBy(), 
            new String[] { "pm1.g1.e1" }, //$NON-NLS-1$ 
            new String[] { "pm1.g1.e1" } ); //$NON-NLS-1$ 
    } 
    
    @Test public void testAliasedOrderBy_ConstantElement() throws Exception {
        Query resolvedQuery = (Query) getCommand("SELECT 0 AS SOMEINT, pm1.g1.e1 as y FROM pm1.g1 ORDER BY y, SOMEINT"); //$NON-NLS-1$
        helpCheckElements(resolvedQuery.getOrderBy(), 
            new String[] { "pm1.g1.e1" }, //$NON-NLS-1$ 
            new String[] { "pm1.g1.e1" } ); //$NON-NLS-1$ 
    } 
    
    @Test public void testOrderByExpression() throws Exception {
    	Query resolvedQuery = (Query) getCommand("SELECT 0 AS SOMEINT, pm1.g1.e2 as y FROM pm1.g1 ORDER BY e2 || e1, e3"); //$NON-NLS-1$
        assertEquals("SELECT X_1.SOMEINT, X_1.Y FROM (SELECT 0 AS SOMEINT, pm1.g1.e2 AS y, (e2 || e1) AS EXPR1, e3 FROM pm1.g1) AS X_1 ORDER BY X_1.EXPR1, X_1.E3", resolvedQuery.toString()); //$NON-NLS-1$
    }
    
    @Test public void testRewiteOrderBy() {
        helpTestRewriteCommand("SELECT 1+1 as a FROM pm1.g1 order by a", "SELECT 2 AS a FROM pm1.g1"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    
    @Test public void testRewiteOrderBy1() {
        helpTestRewriteCommand("SELECT 1+1 as a FROM pm1.g1 union select pm1.g2.e1 from pm1.g2 order by a", "SELECT '2' AS a FROM pm1.g1 UNION SELECT pm1.g2.e1 FROM pm1.g2 ORDER BY a"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    
    @Test public void testOrderByDuplicateRemoval() {
        String sql = "SELECT pm1.g1.e1, pm1.g1.e1 as c1234567890123456789012345678901234567890, pm1.g1.e2 FROM pm1.g1 ORDER BY c1234567890123456789012345678901234567890, e2, e1 "; //$NON-NLS-1$
        helpTestRewriteCommand(sql, "SELECT pm1.g1.e1, pm1.g1.e1 AS c1234567890123456789012345678901234567890, pm1.g1.e2 FROM pm1.g1 ORDER BY c1234567890123456789012345678901234567890, pm1.g1.e2"); //$NON-NLS-1$
    }

}
