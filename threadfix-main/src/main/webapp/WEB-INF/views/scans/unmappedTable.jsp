<%@ include file="/common/taglibs.jsp"%>

	<h4 style="padding-top:8px">Unmapped Findings</h4>

	<spring:url value="/login.jsp" var="loginUrl"/>
	<spring:url value="{scanId}/unmappedTable" var="tableUrl">
		<spring:param name="scanId" value="${ scan.id }"/>
	</spring:url>
	<c:if test="${ numFindings > 100 }">
	<div style="padding-bottom:8px" class="pagination">	
		<ul>
			<c:if test="${ page > 4 }">
				<li>
					<a href="javascript:refillElement('#toReplace2', '${tableUrl}', 1, '<c:out value="${ loginUrl }"/>')">First</a>
				</li>
			</c:if>
		
			<c:if test="${ page >= 4 }">
				<li>
					<a href="javascript:refillElement('#toReplace2', '${tableUrl}', ${ page - 3 }, '<c:out value="${ loginUrl }"/>')"><c:out value="${ page - 3 }"/></a>
				</li>
			</c:if>
		
			<c:if test="${ page >= 3 }">
				<li>
					<a href="javascript:refillElement('#toReplace2', '${tableUrl}', ${ page - 2 }, '<c:out value="${ loginUrl }"/>')"><c:out value="${ page - 2 }"/></a>
				</li>
			</c:if>
			
			<c:if test="${ page >= 2 }">
				<li>
					<a href="javascript:refillElement('#toReplace2', '${tableUrl}', ${ page - 1 }, '<c:out value="${ loginUrl }"/>')"><c:out value="${ page - 1 }"/></a>
				</li>
			</c:if>
			
			<li class="active">
				<a href="#"><c:out value="${ page }"/></a>
			</li>
		
			<c:if test="${ page <= numPages }">
				<li>
					<a href="javascript:refillElement('#toReplace2', '${tableUrl}', ${ page + 1 }, '<c:out value="${ loginUrl }"/>')"><c:out value="${ page + 1 }"/></a>
				</li>
			</c:if>
			
			<c:if test="${ page <= numPages - 1 }">
				<li>
					<a href="javascript:refillElement('#toReplace2', '${tableUrl}', ${ page + 2 }, '<c:out value="${ loginUrl }"/>')"><c:out value="${ page + 2 }"/></a>
				</li>
			</c:if>
			
			<c:if test="${ page <= numPages - 2 }">
				<li>
					<a href="javascript:refillElement('#toReplace2', '${tableUrl}', ${ page + 3 }, '<c:out value="${ loginUrl }"/>')"><c:out value="${ page + 3 }"/></a>
				</li>
			</c:if>
			
			<c:if test="${ page < numPages - 2 }">
				<li>
					<a href="javascript:refillElement('#toReplace2', '${tableUrl}', ${ numPages + 1 }, '<c:out value="${ loginUrl }"/>')">Last (<c:out value="${ numPages + 1}"/>)</a>
				</li>
			</c:if>
		</ul>
		
		<input type="text" class="refillElementOnEnter2" id="pageInput" />
		<a href="javascript:refillElementDropDownPage('#toReplace2', '${ tableUrl }', '<c:out value="${ loginUrl }"/>')">Go to page</a>
	</div>
	</c:if>

	<table class="table" id="1">
		<thead>
			<tr>
				<th class="first">Severity</th>
				<th>Vulnerability Type</th>
				<th>Path</th>
				<th style="min-width:90px;">Parameter</th>
				<th class="last">Number Merged Results</th>
				<th style="width:80px"></th>
			</tr>
		</thead>
		<tbody>
	<c:choose>
		<c:when test="${ empty findingList }">
			<tr class="bodyRow">
				<td colspan="6" style="text-align: center;"> All Findings were mapped to vulnerabilities.</td>
			</tr>
		</c:when>
		<c:otherwise>
		<c:forEach var="finding" items="${ findingList }" varStatus="status">
			<c:if test="${ finding.channelSeverity.numericValue == 5 }">
			      <c:set var="color" value="error" />
			</c:if>
			<c:if test="${ finding.channelSeverity.numericValue == 4 }">
			      <c:set var="color" value="warning" />
			</c:if>
			<c:if test="${ finding.channelSeverity.numericValue == 3 }">
			      <c:set var="color" value="success" />
			</c:if>
			<c:if test="${ finding.channelSeverity.numericValue == 2 }">
			      <c:set var="color" value="info" />
			</c:if>
			<c:if test="${ finding.channelSeverity.numericValue == 1 }">
			      <c:set var="color" value="info" />
			</c:if>
			<tr class="bodyRow <c:out value="${ color }"/>">
				<td id="unmappedSeverity${ status.count }">
					<c:out value="${ finding.channelSeverity.name }"/>
				</td>
				<td id="unmappedVulnerability${ status.count }">
					<c:out value="${ finding.channelVulnerability.name }"/>
				</td>
				<c:if test="${ empty finding.dependency }">
					<td id="unmappedPath${ status.count }"><c:out value="${ finding.surfaceLocation.path }"/></td>
					<td id="unmappedParameter${ status.count }"><c:out value="${ finding.surfaceLocation.parameter }"/></td>
				</c:if>
				<c:if test="${ not empty finding.dependency }">
					<td colspan="2">
						<c:out value="${ finding.dependency.cve } "/>
						(<a target="_blank" id="unmappedcve${ index }" href="http://cve.mitre.org/cgi-bin/cvename.cgi?name=${ finding.dependency.cve }">View</a>)
					</td>
				</c:if>
				<td id="unmappedMergedResults${ status.count }">
					<c:out value="${ finding.numberMergedResults }"/>
				</td>
				<td>
					<spring:url value="{scanId}/findings/{findingId}" var="findingUrl">
					<spring:param name="scanId" value="${ scan.id }" />
						<spring:param name="findingId" value="${ finding.id }" />
					</spring:url>
					<a href="${ fn:escapeXml(findingUrl) }">
						View Finding
					</a>
				</td>
			</tr>
		</c:forEach>
		</c:otherwise>
	</c:choose>
		</tbody>
	</table>
	
	<script>
	$('.refillElementOnEnter2').keypress(function(e) {
		if (e.which == 13) {
			refillElementDropDownPage('#toReplace2', '<c:out value="${ tableUrl }"/>', '<c:out value="${ loginUrl }"/>');
			return false;
		}
	});
	</script>