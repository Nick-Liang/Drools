package com.diligrp.gpurchase.back.rule;

import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;

import com.diligrp.gpurchase.back.dao.GroupOrdersDao;
import com.diligrp.gpurchase.back.domain.orders.GroupOrdersVO;
import com.diligrp.gpurchase.back.rpc.GroupOrderRPC;


public class RuleEngineHelper {
    
    @Resource
    private GroupOrdersDao groupOrdersDao;
    @Resource
    private GroupOrderRPC groupOrderRPC;
    
    public void cancelOrdersWhenPromotionFinalized(Long promotionId, Integer activityStatus){
        System.out.println("invoked");
        List<GroupOrdersVO>  gourpOrderVos = groupOrdersDao.listByProIdAndStatus(promotionId);
        if(!CollectionUtils.isEmpty(gourpOrderVos)){
            for (GroupOrdersVO groupOrdersVO : gourpOrderVos) {
                groupOrderRPC.cancelOrder(groupOrdersVO.getId(), "", "团购活动结束取消订单", activityStatus);
            }
        }
    }
}
