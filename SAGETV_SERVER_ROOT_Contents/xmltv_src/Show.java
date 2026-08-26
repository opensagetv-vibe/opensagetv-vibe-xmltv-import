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

package xmltv;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import sage.EPGDBPublic;

/**
 * POJO for show data.
 * <br>This is an intermediary format between XMLTV and Sage.
 * 
 * @author mta
 */
public class Show {

    /**
     * The start time.
     */
    final Date start;
	
	/**
     * The xmltv channel IconUrl.
     */
    String xmltvIcon;

    /**
     * The previous_start time.
     */
    Date previous_start;

    /**
     * The end time.
     */
    Date end;

    /**
     * The title.
     */
    String title;

    /**
     * The episodeName.
     */
    String episodeName;

    /**
     * The description.
     */
    List<String> descriptions = new LinkedList<String>();

    /**
     * The people.
     */
    List<String> people = new LinkedList<String>();
	
	  /**
     * The characters.
     */
    List<String> characters = new LinkedList<String>();

    /**
     * The roles of the people.
     */
    ByteArrayOutputStream roles = new ByteArrayOutputStream();

    /**
     * The year.
     */
    String year;

    /**
     * The date that the show was finished.
     */
    Date date;

    /**
     * The category.
     */
    LinkedHashSet<String> categories = new LinkedHashSet<String>();

    /**
     * The spoken language.
     */
    String language;

    /**
     * The spoken language of the original.
     */
    String originalLanguage;

    /**
     * The countries where the movie was made.
     */
    Set<String> countries = new LinkedHashSet<String>();

    /**
     * The free form episode number.
     */
    String freeFormEpisodeNumber;

    /**
     * The season.
     */
    int season;

    /**
     * The seasons.
     */
    int seasons;

    /**
     * The episode.
     */
    int episode;

    /**
     * The season.
     */
    int episodes;

    /**
     * The part.
     */
    int part;

    /**
     * The parts.
     */
    int parts;

    /**
     * The rerun indicator.
     * <br>Initially assumes shows are firstrun.
     */
    boolean is_rerun;

    /**
     * The colour string.
     */
    String colour;

    /**
     * The aspect-ratio.
     */
    String aspect;

    /**
     * The quality.
     */
    String quality;

    /**
     * The audio string.
     */
    String audio;
	
	/**
     * The premiere string.
     */
    String premiere;

    /**
     * The premiere indicator.
     */
    boolean is_premiere;
	
	 /**
     * The new indicator.
     */
    boolean is_new;
	
	/**
     * The live indicator.
     */
    boolean is_live;


    /**
     * The subtitles source.
     */
    String subtitles;

    /**
     * The rating.
     */
    String rating;

    /**
     * The expanded ratings.
     */
    Set<String> expandedRatings = new LinkedHashSet<String>();

    /**
     * The number of stars.
     */
    int stars = -1;

    /**
     * The parsed seriesId.
     */
    String seriesId;
	
	/**
     * The parsed showId.
     */
    String showId;
	
	
    /**
     * The parsed parentalRating.
     */	
	String parentalRating="";
    
    /**
     * Creates a new Show.
     * 
     * @param aStart the start time.
     * @param aEnd the end time (can be null).
     */
    public Show(Date aStart, Date aEnd) {
        this.start = aStart;
        this.end = aEnd;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(new SimpleDateFormat("dd-MM-yyyy HH:mm").format(this.start)
                + ": "
                + this.title);
        if (this.episodeName != null) {
            sb.append(" - " + this.episodeName);
        }
        return sb.toString();
		
		
		/*
		
		        return String.format( "ID: %1$12s " , this.showId) + 
			   String.format( "Start: %1$-15s " , new SimpleDateFormat("dd-MM-yyyy HH:mm").format(this.start)) +
			   String.format( "End: %1$-15s " ,   new SimpleDateFormat("dd-MM-yyyy HH:mm").format(this.end)) +
			   String.format( "Numbers: %1$-20s " , numbers) +
			   String.format( "Names: %1$-15s " , names);
			   
		*/	   

    }

    /**
     * Gets the current show's director.
     * 
     * @return the director.
     */
    String getDirector() {
        byte[] r = this.roles.toByteArray();
        for (int i = 0; i < r.length; ++i) {
            if (r[i] == EPGDBPublic.DIRECTOR_ROLE) {
                return this.people.get(i);
            }
        }
        return null;
    }

    /**
     * Gets the lead actors. If these are not specified, the actors are returned
     * instead.
     * 
     * @return the lead actors.
     */
    List<String> getLeadActors() {
        LinkedList<String> list = new LinkedList<String>();
        byte[] r = this.roles.toByteArray();
        for (int i = 0; i < r.length; ++i) {
            switch (r[i]) {
            case EPGDBPublic.LEAD_ACTOR_ROLE:
            case EPGDBPublic.LEAD_ACTRESS_ROLE:
                list.add(this.people.get(i));
            }
        }
        if (list.isEmpty()) {
            for (int i = 0; i < r.length; ++i) {
                switch (r[i]) {
                case EPGDBPublic.ACTOR_ROLE:
                case EPGDBPublic.ACTRESS_ROLE:
                    list.add(this.people.get(i));
                }
            }
        }
        return list;
    }
}
