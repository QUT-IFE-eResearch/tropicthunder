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
    List participants
    String owner
    String contact    
    List siteNames
    // ui specific
    List mapLocation
    
    static mapping = {
        name index:true    
        mapLocation geoIndex:true
    }    
    
    static constraints = {
        
    }
}
