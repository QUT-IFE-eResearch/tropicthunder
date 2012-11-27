package edu.qut.acoustics.tropicthunder.domain

import org.bson.types.ObjectId

/**
 *  Describes the site where sensors are deployed.
 * 
 * @author Shilo Banihit<shiloworks@gmail.com>
 */
class Site {
    ObjectId id    
    String name
//    List location
    
    static mapping = {
        name index:true        
//        location geoIndex:true
    }  
}

