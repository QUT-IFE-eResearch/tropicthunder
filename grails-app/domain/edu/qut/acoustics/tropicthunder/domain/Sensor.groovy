package edu.qut.acoustics.tropicthunder.domain

import org.bson.types.ObjectId

/**
 *
 *  Sensor information
 * 
 *  
 * @author Shilo Banihit shiloworks@gmail.com
 */

class Sensor {
    ObjectId id
    String deviceName
    String manufacturer
    String modelNumber
    String micLeft
    String micRight
    String firmwareVersion
    String deviceId
    String calibration
    
    String micGainLeft
    String micGainRight
    String micHpfLeft
    String micHpfRight
    String divRatio
    String trigLeft
    String trigRight
    String trigWinLeft
    String trigWinRight
    String trigMaxLen
    
    Date calibStartDt
    Date calibEndDt
    
    static mapping = {
        deviceId index:true 
    }
    
    static constraints = {
    }
}
