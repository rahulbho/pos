import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.entity.condition.EntityConditionBuilder;
import org.ofbiz.order.shoppingcart.CheckOutHelper;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.order.order.OrderReadHelper;

def initialiseOrder() {
    locale = (Locale) context.get("locale");
    currencyUomId = "USD";
    userLogin = delegator.findOne("UserLogin", false, [userLoginId: "posAnonymous"]);
    cart = new ShoppingCart(delegator, "9100", locale, currencyUomId);
    cart.setOrderType("SALES_ORDER");
    /*TODO: use POS_SALES_CHANNEL instead*/
    cart.setChannelType("WEB_SALES_CHANNEL");
    partyId = "_NA_";

    cart.setOrderPartyId(partyId);
    cart.setShipmentMethodTypeId(0, "NO_SHIPPING");
    cart.setAllCarrierPartyId("_NA_");
    checkout = new CheckOutHelper(dispatcher, delegator, cart);
    orderCreateResult = checkout.createOrder(userLogin);

    result = ServiceUtil.returnSuccess();
    result.orderId = orderCreateResult.orderId;
    return result;
}
def addItem() {
    userLogin = delegator.findOne("UserLogin", false, [userLoginId: "posAnonymous"]);
    orderId = context.orderId;
    productId = context.productId;

    appendOrderItemCtx = [userLogin: userLogin, orderId: orderId, quantity: BigDecimal.ONE, productId: productId, shipGroupSeqId:"00001"];
    serviceResultMap = dispatcher.runSync("appendOrderItem", appendOrderItemCtx);
    result = ServiceUtil.returnSuccess();
    result.orderId = serviceResultMap.orderId;
    return result;
}
def editOrderItem() {
    userLogin = delegator.findOne("UserLogin", false, [userLoginId: "posAnonymous"]);
    orderId = context.orderId;
    orderItemSeqId = context.orderItemSeqId;
    quantity = context.quantity;
    unitPrice = context.unitPrice;
    /*prepare input maps*/
    overridePriceMap = [:];
    overridePriceMap[orderItemSeqId] = "Y";
    itemQtyMap = [:];
    itemQtyMapKey = orderItemSeqId+":00001";
    itemQtyMap[itemQtyMapKey] = quantity;
    itemPriceMap = [:];
    itemPriceMap[orderItemSeqId] = unitPrice;
    updateOrderItemsCtx = [userLogin: userLogin, orderId: orderId, overridePriceMap: overridePriceMap, itemQtyMap: itemQtyMap, itemPriceMap: itemPriceMap];
    dispatcher.runSync("updateOrderItems", updateOrderItemsCtx);
    result = ServiceUtil.returnSuccess();
    return result;
}
def getOrderDetails() {
    userLogin = delegator.findOne("UserLogin", false, [userLoginId: "posAnonymous"]);
    orderId = context.orderId;
    exprBldr = new EntityConditionBuilder();
    orderHeader = delegator.findOne("OrderHeader", false, [orderId: orderId]);
    orderItems = delegator.findList("OrderItem", exprBldr.EQUALS(orderId: orderId), null, null, null, false);
    orderAdjustments = delegator.findList("OrderAdjustment", exprBldr.EQUALS(orderId: orderId), null, null, null, false);
    orderReadHelper = new OrderReadHelper(orderHeader, orderAdjustments, orderItems);

    validOrderItems = orderReadHelper.getValidOrderItems();
    itemList = [];
    validOrderItems.each { validOrderItem ->
        item = [:];
        item.orderItemSeqId = validOrderItem.orderItemSeqId;
        item.itemDescription = validOrderItem.itemDescription;
        item.productId = validOrderItem.productId;
        item.quantity = validOrderItem.quantity;
        item.unitPrice = validOrderItem.unitPrice;
        itemList.add(item);
    }
    /*print "=========validOrderItems========="+validOrderItems+"\n=======================orderItems========="+orderItems;*/
    result =ServiceUtil.returnSuccess();
    orderDetails = [:];
    orderDetails.itemList = itemList;
    orderDetails.subTotal = orderReadHelper.getOrderItemsSubTotal();
    orderDetails.grandTotal = orderReadHelper.getOrderGrandTotal();
    orderDetails.taxTotal = orderReadHelper.getTaxTotal();
    result.orderDetails = orderDetails;
    return result;
}