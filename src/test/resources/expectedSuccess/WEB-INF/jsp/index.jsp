<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %><%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%><c:url value="/img/db70ac9dda4df0c72b756fbcf610edcaimg.png" var="faviconUrl"/><!-- multiline --><c:url value="/img/db70ac9dda4df0c72b756fbcf610edcaimg.png"var="faviconUrl"/><!-- jsp variables preserved and won't fingerprint resource --><c:url value="${pageContext.request.contextPath}/img/img.png" var="faviconUrl"/><!-- full urls ignored --><c:url value="http://www.fedex.com/Tracking?ascend_header=1&amp;clienttype=dotcom&amp;cntry_code=us&amp;language=english&amp;tracknumbers=${shipment.trackingNumber}" var="faviconUrl"/>