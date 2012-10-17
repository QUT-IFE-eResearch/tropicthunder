package edu.qut.acoustics.tropicthunder.domain

import org.bson.types.ObjectId

/**
 *
 *  Acoustics Project information
 *  
 * @author Shilo Banihit shiloworks@gmail.com
 */

class Project {
    ObjectId id  
    String name
    String title
    String description
    List<String> participants
    String owner
    String contact    
    List<String> siteNames
    
    static mapping = {
        name index:true    
    }    
    
    static constraints = {
    }
}
