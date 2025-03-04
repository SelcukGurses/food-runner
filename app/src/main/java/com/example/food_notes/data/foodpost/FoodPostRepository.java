package com.example.food_notes.data.foodpost;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;

public class FoodPostRepository implements FoodPostDataSource {

    private final FoodPostDao mFoodPostDao;

    public FoodPostRepository(FoodPostDao foodPostDao) {
        mFoodPostDao = foodPostDao;
    }

    @Override
    public Flowable<List<FoodPost>> getAllData() {
        return mFoodPostDao.getAllPosts();
    }

    @Override
    public Completable insertOrUpdate(FoodPost foodPost) {
        return mFoodPostDao.insertOrUpdate(foodPost);
    }

    @Override
    public Flowable<FoodPost> getFoodPost(Long id) {
        return mFoodPostDao.getFoodPostById(id);
    }

    @Override
    public Completable deleteAllData() {
        return mFoodPostDao.deleteAllPosts();
    }

    @Override
    public Completable deleteFoodPostById(Long id) {
        return mFoodPostDao.deleteFoodPostById(id);
    }

}
