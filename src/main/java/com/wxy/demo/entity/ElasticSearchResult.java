package com.wxy.demo.entity;

/**
 * @Author wxy
 * @Date 19-8-26 下午4:02
 * @Description TODO
 **/
public class ElasticSearchResult {
    private double score;

    private String name;

    public ElasticSearchResult() {
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
