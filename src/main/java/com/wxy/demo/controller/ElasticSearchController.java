package com.wxy.demo.controller;

import com.wxy.demo.entity.ElasticSearchResult;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
public class ElasticSearchController {

    @Autowired
    private TransportClient client;

    //搜索自动联想
    @GetMapping("/auto_think.do")
    public List<ElasticSearchResult> findIndexRecordByName(@RequestParam(name = "key") String key) {
        // 构造查询请求
        QueryBuilder bq = QueryBuilders.matchQuery("name.pinyin", key);
        SearchRequestBuilder searchRequest = client.prepareSearch("medcl").setTypes("folks");

        // 设置查询条件和分页参数
        int start = 0;
        int size = 5;
        searchRequest.setQuery(bq).setFrom(start).setSize(size);

        // 获取返回值，并进行处理
        SearchResponse response = searchRequest.execute().actionGet();
        SearchHits shs = response.getHits();
        List<ElasticSearchResult> esResultList = new ArrayList<>();
        for (SearchHit hit : shs) {
            ElasticSearchResult esResult = new ElasticSearchResult();
            double score = hit.getScore();
            BigDecimal b = new BigDecimal(score);
            score = b.setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
            String name = (String) hit.getSourceAsMap().get("name");
            System.out.println("score:" + score + "name:" + name);
            esResult.setScore(score);
            esResult.setName(name);
            esResultList.add(esResult);
        }
        return esResultList;
    }

    /**
     * 添加文档
     * @param name
     */
    public void addByName(String name) {
        try {
            XContentBuilder content = XContentFactory.jsonBuilder()
                    .startObject().field("name", name).endObject();
            client.prepareIndex("medcl", "folks")
                    .setSource(content).get();
            System.out.println("ElasticSearch添加文档成功。");
        } catch (IOException e) {
            System.out.println("ElasticSearch添加文档出错。");
            e.printStackTrace();
        }
    }

    /**
     * 删除文档
     * @param name
     */
    public void deleteByName(String name) {
        BulkRequestBuilder bulkRequest = client.prepareBulk();
        SearchResponse response = client.prepareSearch("medcl").setTypes("folks")
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(QueryBuilders.termQuery("name", name))
                .setFrom(0).setSize(20).setExplain(true).execute().actionGet();
        for(SearchHit hit : response.getHits()){
            String id = hit.getId();
            bulkRequest.add(client.prepareDelete("medcl", "folks", id).request());
        }
        BulkResponse bulkResponse = bulkRequest.get();

        if (bulkResponse.hasFailures()) {
            System.out.println("ElasticSearch删除文档出错。");
            for(BulkItemResponse item : bulkResponse.getItems()){
                System.out.println(item.getFailureMessage());
            }
        }else {
            System.out.println("ElasticSearch删除文档成功。");
        }
    }

    /**
     * 更新文档
     * @param beforeName
     * @param afterName
     */
    public void updateByName(String beforeName,String afterName){
        SearchResponse response = client.prepareSearch("medcl").setTypes("folks")
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(QueryBuilders.termQuery("name", beforeName))
                .setFrom(0).setSize(20).setExplain(true).execute().actionGet();
        try {
            for(SearchHit hit : response.getHits()){
                UpdateRequest updateRequest = new UpdateRequest();
                updateRequest.index("medcl");
                updateRequest.type("folks");
                updateRequest.id(hit.getId());
                updateRequest.doc(XContentFactory.jsonBuilder().startObject().field("name",afterName).endObject());
                client.update(updateRequest).get();
            }
            System.out.println("ElasticSearch更新文档成功。");
        }catch (Exception e){
            System.out.println("ElasticSearch更新文档出错。");
            e.printStackTrace();
        }
    }

}