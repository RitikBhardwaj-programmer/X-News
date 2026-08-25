package com.cfs.xnews.news.articles;

public interface ArticleSimilarityProjection {

    Article getArticle();

    double getSimilarity();
}