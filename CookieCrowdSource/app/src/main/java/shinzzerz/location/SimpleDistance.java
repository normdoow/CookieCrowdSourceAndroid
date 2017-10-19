package shinzzerz.location;

import java.lang.reflect.Field;

/**
 * Created by administratorz on 10/15/2017.
 */

public class SimpleDistance {
    private static final double MILE_PER_KILOMETER = 0.621371;
    private static final double METER_PER_KILOMETER = 1000;
    private DistTypeEnum currentUnit;
    private double miles;
    private double kilometers;
    private double meters;

    public SimpleDistance(){
        this(0.0, DistTypeEnum.Miles);
    }


    public SimpleDistance(double distance, DistTypeEnum distanceUnits)  {
        //Try setting the distance via introspection
        try{
            Field privateMember = this.getClass().getField(distanceUnits.toString().toLowerCase());
            privateMember.setAccessible(true);
            privateMember.set(this, distance);

            currentUnit = distanceUnits;
        }
        catch(IllegalAccessException|NoSuchFieldException ex){
            currentUnit = distanceUnits;
            switch (distanceUnits) {
                case Kilometers:
                    kilometers = distance;
                    break;
                case Meters:
                    meters = distance;
                    break;
                case Miles:
                    miles = distance;
                    break;
            }
        }

    }

    public void setDistance(double distance, DistTypeEnum distanceUnits){
        try{
            Field privateMember = this.getClass().getField(distanceUnits.toString().toLowerCase());
            privateMember.setAccessible(true);
            privateMember.set(this, distance);

            currentUnit = distanceUnits;
        }
        catch(IllegalAccessException|NoSuchFieldException ex){
            switch (distanceUnits) {
                case Kilometers:
                    kilometers = distance;
                    currentUnit = DistTypeEnum.Kilometers;
                    break;
                case Meters:
                    meters = distance;
                    currentUnit = DistTypeEnum.Meters;
                    break;
                case Miles:
                    miles = distance;
                    currentUnit = DistTypeEnum.Miles;
                    break;
            }
        }
    }

    public double getDistance(DistTypeEnum wantedDistanceInUnit) {
       return doConversion(wantedDistanceInUnit);
    }

    private double doConversion(DistTypeEnum wantedDistanceInUnit){
        switch (currentUnit) {
            case Kilometers:
                if(currentUnit == wantedDistanceInUnit){
                    return kilometers;
                }
                else{
                    miles = kilometers * MILE_PER_KILOMETER;
                    meters = kilometers * METER_PER_KILOMETER;
                    return wantedDistanceInUnit == DistTypeEnum.Meters ? meters : miles;
                }
            case Meters:
                if(currentUnit == wantedDistanceInUnit){
                    return meters;
                }
                else{
                    kilometers = meters * (1/METER_PER_KILOMETER);
                    miles = kilometers * MILE_PER_KILOMETER;
                    return wantedDistanceInUnit == DistTypeEnum.Kilometers ? kilometers : miles;
                }
            case Miles:
                if(currentUnit == wantedDistanceInUnit){
                    return miles;
                }
                else{
                    kilometers = miles * (1/MILE_PER_KILOMETER);
                    meters = kilometers * METER_PER_KILOMETER;
                    return wantedDistanceInUnit == DistTypeEnum.Kilometers ? kilometers : meters;
                }
            default:
                return Double.POSITIVE_INFINITY;
        }
    }




//    public SimpleDistance(double distance , DistTypeEnum distanceUnits){
////        if(distanceUnits.equals(DistTypeEnum.Kilometers)){
////            kilometers = distance;
////            meters = distance * METER_PER_KILOMETER;
////            miles = distance * MILE_PER_KILOMETER;
////        }
////        else if(distanceUnits.equals(DistTypeEnum.Meters)){
////            meters = distance;
////            kilometers = distance * (1/METER_PER_KILOMETER);
////            miles = kilometers * MILE_PER_KILOMETER;
////        }
////        else{
////            miles = distance;
////            kilometers = miles * (1/MILE_PER_KILOMETER);
////            meters = kilometers * (MILE_PER_KILOMETER);
////        }
////    }
////
////    public double getDistance(DistTypeEnum distanceUnits){
////        if(distanceUnits.equals(DistTypeEnum.Kilometers)){
////            return kilometers;
////        }
////        else if(distanceUnits.equals(DistTypeEnum.Meters)){
////            return meters;
////        }
////        else{
////            return miles;
////        }
////    }
////
////    public void setDistance(double distance, DistTypeEnum distanceUnits){
////        if(distanceUnits.equals(DistTypeEnum.Kilometers)){
////            kilometers = distance;
////            meters = distance * METER_PER_KILOMETER;
////            miles = distance * MILE_PER_KILOMETER;
////        }
////        else if(distanceUnits.equals(DistTypeEnum.Meters)){
////            meters = distance;
////            kilometers = distance * (1/METER_PER_KILOMETER);
////            miles = kilometers * MILE_PER_KILOMETER;
////        }
////        else{
////            miles = distance;
////            kilometers = miles * (1/MILE_PER_KILOMETER);
////            meters = kilometers * (METER_PER_KILOMETER);
////        }
//
//
//    }


}
