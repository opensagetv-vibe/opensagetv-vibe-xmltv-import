/*
 * Copyright 2016 The XMLTVImportPlugin for SageTV Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

 /* Change Log
##2.03  01/14/2022
1.  Added String xmltvIcon to store Channel ICON

 */
package xmltv;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.LinkedList;
import java.util.List;
import java.util.Date;

/**
 * POJO for channel data.
 * <br>This is an intermediary format between XMLTV and Sage.
 * 
 * @author mta
 */
public final class Channel {

    /**
     * The xmltv channel id. <channel id="xmltvId">
     */
    final String xmltvId;

	/**
     * The xmltv channel <icon src="xmltvIcon" />
     */
    String xmltvIcon;

	/**
     * The xmltv channel <display-name>xmltvDisplayName</display-name>
     */
    List<String> xmltvDisplayNames= new LinkedList<String>();
	
	/**
     * The xmltv this.init.ChannelNumberTag 
	 *  <this.init.ChannelNumberTag>2.1</this.init.ChannelNumberTag>
	 *  this.init.ChannelNumberTag=lcn  <lcn>2.1</lcn>
     */
    String xmltvNumberTag;
	
    /**
     * The ShortName of the channel.
     */
    String network = "";

    /**
     * The tuner channel numbers.   
     */
    LinkedHashSet<String> numbers = new LinkedHashSet<String>();

    /**
     * The station id.  This is the SageTV station ID used for it's database
     */ 
    Integer STVstationID;

    /**
     * The guids that have been used for movies on this channel.
     */
    HashMap<String, Date> movieIds = new HashMap<String, Date>();

    /** 
     * Creates a new instance of Channel
     *  
     * @param aXmltvId the XMLTV id for the channel.
     */
    public Channel(String aXmltvId) {
        this.xmltvId = aXmltvId;
    }

}
