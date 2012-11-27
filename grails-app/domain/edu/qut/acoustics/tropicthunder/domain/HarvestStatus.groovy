package edu.qut.acoustics.tropicthunder.domain

import org.bson.types.ObjectId

class HarvestStatus {

    ObjectId id
    boolean running
    Date startDt
    Date endDt
    List currentRecordings
    
    static mapping = {
        
    }
    
    static constraints = {
    }
}
