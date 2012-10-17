package edu.qut.acoustics.tropicthunder.domain

import org.bson.types.ObjectId

/**
 *
 *  Acoustics Portal Configuration
 *  
 * @author Shilo Banihit shiloworks@gmail.com
 */

class ConfigPortal {
    ObjectId id  
    
    String source
    Date startEffectiveDt
    Date endEffectiveDt
    Map settings
    
    static mapping = {
        startEffectiveDt index:true
        endEffectiveDt index:true
    }
    
    static constraints = {        
    }
}
