package Utils;

import java.util.List;

import Models.PointType;

public interface SingletonInterface {
    default void onPointTypeComplete(List<PointType> data){}
    default void onPostSuccess(){}
    default void onError(Exception e){}
}