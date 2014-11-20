import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.condition.EntityConditionBuilder;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.service.GenericServiceException
import org.ofbiz.order.shoppingcart.CheckOutHelper;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.service.ServiceUtil;

module = "PosServices.groovy";
locale = (Locale) context.get("locale");

def initialiseOrder() {
    resource = "PosUiLabels";
    currencyUomId = "USD";
    /*TODO: Final Validation logic will go here*/
    try {
        userLogin = delegator.findOne("UserLogin", false, [userLoginId: "posAnonymous"]);
    } catch (GenericEntityException e) {
        errorMessage = "Error in getting user login!!";
        Debug.logError(errorMessage + e.getMessage(), module);
        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "PosUnableToAccessSystem", locale));
    }

    /*TODO: use dynamic value of productStoreId */
    cart = new ShoppingCart(delegator, "9100", locale, currencyUomId);
    if (cart) {
        cart.setOrderType("SALES_ORDER");
        cart.setChannelType("POS_SALES_CHANNEL");
        partyId = "_NA_";

        cart.setOrderPartyId(partyId);
        cart.setShipmentMethodTypeId(0, "NO_SHIPPING");
        cart.setAllCarrierPartyId("_NA_");
        checkout = new CheckOutHelper(dispatcher, delegator, cart);
        orderCreateResult = checkout.createOrder(userLogin);

        result = ServiceUtil.returnSuccess();
        result.orderId = orderCreateResult.orderId;
        return result;
    } else {
        Debug.logError("Error! Unable to get the cart object!!", module);
        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ErrorUiLabel", locale));
    }
}
def addItem() {
    resource = "PosUiLabels";
    /*TODO: Final Validation logic will go here*/
    try {
        userLogin = delegator.findOne("UserLogin", false, [userLoginId: "posAnonymous"]);
    } catch (GenericEntityException e) {
        errorMessage = "Error in getting user login!!";
        Debug.logError(errorMessage + e.getMessage(), module);
        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "PosUnableToAccessSystem", locale));
    }
    orderId = context.orderId;
    productId = context.productId;

    appendOrderItemCtx = [userLogin: userLogin, orderId: orderId, quantity: BigDecimal.ONE, productId: productId, shipGroupSeqId:"00001"];
    try {
        serviceResult = dispatcher.runSync("appendOrderItem", appendOrderItemCtx);
    } catch (GenericServiceException e) {
        errorMessage = "Error! Unable to add Item in Order!!";
        Debug.logError(errorMessage + e.getMessage(), module);
        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "PosUnableToAddItem", locale));
    }
    if (orderId.equals(serviceResult.orderId)) {
        result = ServiceUtil.returnSuccess();
        return result;
    } else {
        Debug.logError("Error! Unable to add Item in Order!!", module);
        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "PosUnableToAddItem", locale));
    }
}
def editOrderItem() {
    try {
        userLogin = delegator.findOne("UserLogin", false, [userLoginId: "posAnonymous"]);
    } catch (GenericEntityException e) {
        errorMessage = "Error in getting user login!!";
        Debug.logError(errorMessage + e.getMessage(), module);
        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "PosUnableToAccessSystem", locale));
    }
    orderId = context.orderId;
    orderItemSeqId = context.orderItemSeqId;
    unitPrice = context.unitPrice;
    quantity = context.quantity;

    /*To handle quantity=0 or some invalid value*/
    qty = quantity as int;
    if (qty == 0) {
        cancelOrderItemCtx = [userLogin: userLogin, orderItemSeqId: orderItemSeqId, orderId: orderId];
        try {
            dispatcher.runSync("cancelOrderItem", cancelOrderItemCtx);
        } catch (GenericServiceException e) {
            errorMessage = "Error! Unable to remove Item from Order!!";
            Debug.logError(errorMessage + e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "PosUnableToRemoveItem", locale));
        }
        result = ServiceUtil.returnSuccess();
        return result;
    } else if (qty < 0) {
        rrorMessage = "Invalid Quantity!! Please Enter Valid Quantity";
        Debug.logError(errorMessage + e.getMessage(), module);
        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "PosPleaseEnterValidQuantity", locale));
    } else {
        /*prepare input maps as per the service requirements*/
        overridePriceMap = [:];
        overridePriceMap[orderItemSeqId] = "N";
        itemQtyMap = [:];
        itemQtyMapKey = orderItemSeqId+":00001";
        itemQtyMap[itemQtyMapKey] = quantity;
        itemPriceMap = [:];
        itemPriceMap[orderItemSeqId] = unitPrice;
        updateOrderItemsCtx = [userLogin: userLogin, orderId: orderId, overridePriceMap: overridePriceMap, itemQtyMap: itemQtyMap, itemPriceMap: itemPriceMap];
        try {
            dispatcher.runSync("updateOrderItems", updateOrderItemsCtx);
        } catch (GenericServiceException e) {
            errorMessage = "Error! Unable to edit Item in Order!!";
            Debug.logError(errorMessage + e.getMessage(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "PosUnableToEditItem", locale));
        }
        result = ServiceUtil.returnSuccess();
        return result;
    }
}
def deleteOrderItem() {
    try {
        userLogin = delegator.findOne("UserLogin", false, [userLoginId: "posAnonymous"]);
    } catch (GenericEntityException e) {
        errorMessage = "Error in getting user login!!";
        Debug.logError(errorMessage + e.getMessage(), module);
        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "PosUnableToAccessSystem", locale));
    }
    orderId = context.orderId;
    orderItemSeqId = context.orderItemSeqId;
    cancelOrderItemCtx = [userLogin: userLogin, orderItemSeqId: orderItemSeqId, orderId: orderId];
    try {
        dispatcher.runSync("cancelOrderItem", cancelOrderItemCtx);
    } catch (GenericServiceException e) {
        errorMessage = "Error! Unable to remove Item from Order!!";
        Debug.logError(errorMessage + e.getMessage(), module);
        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "PosUnableToRemoveItem", locale));
    }
    result = ServiceUtil.returnSuccess();
    return result;
}
def getOrderDetails() {
    try {
        userLogin = delegator.findOne("UserLogin", false, [userLoginId: "posAnonymous"]);
    } catch (GenericEntityException e) {
        errorMessage = "Error in getting user login!!";
        Debug.logError(errorMessage + e.getMessage(), module);
        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "PosUnableToAccessSystem", locale));
    }
    orderId = context.orderId;
    exprBldr = new EntityConditionBuilder();
    try {
        orderHeader = delegator.findOne("OrderHeader", false, [orderId: orderId]);
        orderItems = delegator.findList("OrderItem", exprBldr.EQUALS(orderId: orderId), null, null, null, false);
        orderAdjustments = delegator.findList("OrderAdjustment", exprBldr.EQUALS(orderId: orderId), null, null, null, false);
    } catch (GenericEntityException e) {
        errorMessage = "Error in getting details of order!!";
        Debug.logError(errorMessage + e.getMessage(), module);
        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "PosUnableToFetchOrder", locale));
    }
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
    orderDetails = [:];
    orderDetails.itemList = itemList;
    orderDetails.subTotal = orderReadHelper.getOrderItemsSubTotal();
    orderDetails.grandTotal = orderReadHelper.getOrderGrandTotal();
    orderDetails.taxTotal = orderReadHelper.getTaxTotal();
    result =ServiceUtil.returnSuccess();
    result.orderDetails = orderDetails;
    return result;
}