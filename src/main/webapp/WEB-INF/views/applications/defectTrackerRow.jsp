<%@ include file="/common/taglibs.jsp"%>

<td>Defect Tracker</td>
<c:choose>
	<c:when test="${ empty application.defectTracker }">
		<td class="inputValue">
			<a href="#addDefectTracker" role="button" class="btn" data-toggle="modal">Add Defect Tracker</a>
		</td>
	</c:when>
	<c:otherwise>
		<td class="inputValue">
			<spring:url value="/configuration/defecttrackers/{defectTrackerId}" var="defectTrackerUrl">
				<spring:param name="defectTrackerId" value="${ application.defectTracker.id }"/>
			</spring:url>
			<a id="defectTrackerText" href="${ fn:escapeXml(defectTrackerUrl) }"><c:out value="${ application.defectTracker.name }"/></a>
			<em>(<a href="<spring:url value="${ fn:escapeXml(application.defectTracker.url) }" />"><c:out value="${ fn:escapeXml(application.defectTracker.url) }"/></a>)</em>
		</td>
		<td>
			<a href="#addDefectTracker" role="button" class="btn" data-toggle="modal">Edit Defect Tracker</a>
		</td>
	</c:otherwise>
</c:choose>