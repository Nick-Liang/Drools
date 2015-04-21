package com.diligrp.gpurchase.back.rule;

import java.util.List;

import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.StatelessKieSession;
import org.springframework.beans.factory.InitializingBean;

import com.diligrp.gpurchase.back.dao.PromotionProductDao;
import com.diligrp.gpurchase.back.dao.PromotionRuleDao;
import com.diligrp.gpurchase.back.domain.PromotionProduct;
import com.diligrp.gpurchase.back.domain.PromotionRule;
import com.diligrp.gpurchase.back.manager.promotionProduct.PromotionProductManager;
import com.diligrp.gpurchase.back.rpc.ProductRPC;
import com.diligrp.titan.sdk.domain.Product;
import com.diligrp.titan.sdk.domain.Sku;
import com.diligrp.website.util.dao.BaseQuery;

public class RuleEngine implements InitializingBean{
    
    private PromotionRuleDao promotionRuleDao ;
    
    private PromotionProductDao promotionProductDao;
    
    private RuleEngineHelper ruleEngineHelper;
    
    private KieContainer kContainer = null;
    
    private ProductRPC productRPC;
    /**
     * 
     * this method is 通过任务调度系统定时触发，判断活动商品开始时间和结束时间
     * @createTime 2014-12-30 上午11:35:50
     * @author Nick
     */
    public void purchaseTimeRule(){
        
        StatelessKieSession kSession = kContainer.newStatelessKieSession("session-clock");
        /*
         * 扫描目前的活动商品PromotionProduct对应的时间规则
         */
        List<PromotionRule> productRuleList =  promotionRuleDao.findPromotionRuleTime(null);
        /*
         * 设置公共的Dao进入WorkingMemory
         */
        kSession.setGlobal("promotionProductDaoImpl", promotionProductDao);
        kSession.setGlobal("ruleEngineHelper", ruleEngineHelper);
        /*
         * 设置公共的系统时间进入WorkingMemory
         */
        kSession.setGlobal("currentTime", System.currentTimeMillis());
        /*
         * 执行Rules
         */
        kSession.execute(productRuleList);
    }
    
    /**
     * 
     * this method is 在订单付款成功时回调，判断是否满足成图和结束条件
     * @createTime 2014-12-30 上午11:35:50
     * @author Nick
     */
    public void purchaseOrdersRule(long id){
        
        KieSession kSession = kContainer.newKieSession("session-orders");
        
        /*
         * 查询PromotionProduct对应的规则
         */
        BaseQuery bq = new BaseQuery();
        bq.addParam("promotionId", String.valueOf(id));
        PromotionRule promotionRule =  promotionRuleDao.findPromotionRuleByPromotionId(bq);
        /*
         * 设置公共的Dao进入WorkingMemory
         */
        kSession.setGlobal("promotionProductDaoImpl", promotionProductDao);
        kSession.setGlobal("ruleEngineHelper", ruleEngineHelper);
        /*
         * 查询PromotionProduct已付款订单数
         */
        Integer totalQ = promotionProductDao.sumOrderCountByProId(id);
        /*
         * 查询PromotionProduct已付款的购买数量及可售量
         */
        PromotionProduct pp = promotionProductDao.sumOrderByProId(id);
        if(pp.getSaleNum() == null){
            pp.setSaleNum(0);
        }
        /*
         * 查询PromotionProduct已付款的购买数量及可售量
         */
        Product product = productRPC.getProductInfo(pp.getProductId(), 1);
        List<Sku> skuList = product.getSkus();
        Boolean isLowStockAll = true;
        for (Sku sku : skuList) {
            if(sku.getStockNum() >= sku.getMinNum()){
                isLowStockAll = false;
                break;
            }
        }
        kSession.setGlobal("orderQuantity", totalQ + promotionRule.getVirtualPeople());
        kSession.setGlobal("isLowStockAll", isLowStockAll);
        
        /*
         * 插入对象进入WorkingMemory
         */
        kSession.insert(pp);
        kSession.insert(product);
        kSession.insert(promotionRule);
        /*
         * 执行Rules
         */
        kSession.fireAllRules();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        
        KieServices kieService = KieServices.Factory.get();
        
        kContainer = kieService.getKieClasspathContainer();
    }

    
    public void setPromotionRuleDao(PromotionRuleDao promotionRuleDao) {
        this.promotionRuleDao = promotionRuleDao;
    }
    
    public void setPromotionProductDao(PromotionProductDao promotionProductDao) {
        this.promotionProductDao = promotionProductDao;
    }
    
    public void setProductRPC(ProductRPC productRPC) {
        this.productRPC = productRPC;
    }
    
    public void setRuleEngineHelper(RuleEngineHelper ruleEngineHelper) {
        this.ruleEngineHelper = ruleEngineHelper;
    }
    
    
}
