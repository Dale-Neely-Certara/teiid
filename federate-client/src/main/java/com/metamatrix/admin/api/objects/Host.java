/*
 * JBoss, Home of Professional Open Source.
 * Copyright (C) 2008 Red Hat, Inc.
 * Copyright (C) 2000-2007 MetaMatrix, Inc.
 * Licensed to Red Hat, Inc. under one or more contributor 
 * license agreements.  See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package com.metamatrix.admin.api.objects;



/**
 * Represents a host in the MetaMatrix system.
 *
 * <p>The identifier pattern for a host is <code>"hostName"</code>.
 * A hostName is concidered to be unique across the system.</p>
 * @since 4.3
 */
public interface Host extends AdminObject {
    


    /**
     * Port Number Property
     */
    public static final String PORT_NUMBER = "hostControllerPortNumber"; //$NON-NLS-1$ HostType.PORT_NUMBER; 
    
    /**
     * Install Directory Property
     */
    public static final String INSTALL_DIR = "metamatrix.installationDir"; //$NON-NLS-1$ -- HostType.INSTALL_DIR;
    
    /**
     * Log Directory Property
     */
    public static final String LOG_DIRECTORY = "metamatrix.log.dir"; //$NON-NLS-1$ HostType.LOG_DIRECTORY; 
    
    /**
     * Data Directory Property
     */
    public static final String DATA_DIRECTORY = "metamatrix.data.dir"; //$NON-NLS-1$ HostType.DATA_DIRECTORY; 
    
    /**
     * Host Directory Property
     */
    public static final String HOST_DIRECTORY = "metamatrix.host.dir"; //$NON-NLS-1$ HostType.HOST_DIRECTORY; 
    
    /**
     * Host Enabled Property
     */
    public static final String HOST_ENABLED = "host.enabled"; //$NON-NLS-1$ HostType.HOST_ENABLED; 
    
    /**
     * Host Bind Address Property
     */
    public static final String HOST_BIND_ADDRESS = "metamatrix.host.bind.address"; //$NON-NLS-1$ HostType.HOST_BIND_ADDRESS; 
    
    /**
     * Host Physical Address Property
     */
    public static final String HOST_PHYSICAL_ADDRESS = "metamatrix.host.physical.address"; //$NON-NLS-1$ HostType.HOST_PHYSICAL_ADDRESS; 
    

    /**
     * Return true if this Host is executing.
     * 
     * @return if this Host is actively participating
     * in the MetaMatrix system.
     * @since 4.3
     */
    public boolean isRunning();
}
