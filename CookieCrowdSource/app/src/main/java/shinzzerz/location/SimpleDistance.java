package shinzzerz.location;

/**
 * Created by administratorz on 10/15/2017.
 */

public class SimpleDistance {
    private static final double MILE_PER_KILOMETER = 0.621371;
    private static final int METER_PER_KILOMETER = 1000;
    private DistTypeEnum currentUnit;
    private double miles;
    private double kilometers;
    private double meters;


    public SimpleDistance(double distance, DistTypeEnum distanceUnits) {
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

    public double getDistance(DistTypeEnum distanceUnits) {
//        double toReturn;
//        switch (currentUnit) {
//            case Kilometers:
//                break;
//            case Meters:
//                meters = distance;
//                currentUnit = DistTypeEnum.Meters;
//                break;
//            case Miles:
//                miles = distance;
//                currentUnit = DistTypeEnum.Miles;
//                break;
//        }
        return 0.0;
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
