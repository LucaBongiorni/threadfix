<%@ include file="/common/taglibs.jsp"%>

<head>
	<title>Manage Roles</title>
</head>

<body>
	<h2>Manage Roles</h2>

	<c:if test="${ not empty error }">
		<span class="errors"><c:out value="${ error }" /></span>
	</c:if>

	<div id="helpText">
		ThreadFix Roles determine functional capabilities for associated users.<br/>
	</div>
	
	<table class="formattedTable">
		<thead>
			<tr>
				<th class="medium first">Name</th>
				<th class="short">Edit</th>
				<th class="short">Delete</th>
				<th class="medium">Configure Users</th>
			</tr>
		</thead>
		<tbody>
			<c:if test="${ empty roleList }">
				<tr class="bodyRow">
					<td colspan="6" style="text-align:center;">No roles found.</td>
				</tr>
			</c:if>
			<c:forEach var="model" items="${ roleList }" varStatus="status">
				<tr class="bodyRow">
					<td id="role${ status.count }">
						<c:out value="${ model.role.displayName }"/>
					</td>
					<td>
						<spring:url value="/configuration/roles/{roleId}/edit" var="roleEditUrl">
							<spring:param name="roleId" value="${ model.role.id }" />
						</spring:url>
						<a id="edit${ status.count }" href="${ fn:escapeXml(roleEditUrl) }">Edit</a> 
					</td>
					<td>
						<spring:url value="/configuration/roles/{roleId}/delete" var="roleDeleteUrl">
							<spring:param name="roleId" value="${ model.role.id }" />
						</spring:url>
						<form:form method="POST" action="${ fn:escapeXml(roleDeleteUrl) }">
							<input id="delete${ status.count }" type="submit"
							
							<c:if test="${ model.canDelete }">
							onclick="return confirm('Are you sure you want to delete this Role? All users will have their privileges revoked.')" 
							</c:if>
							
							<c:if test="${ not model.canDelete }">
							onclick="alert('This role cannot be deleted because it is the last role with permissions to manage either groups, users, or roles.'); return false;" 
							</c:if>
							
							value="Delete"/>
						</form:form>
					</td>
					<td>
						<spring:url value="/configuration/roles/{roleId}/users" var="userConfigUrl">
							<spring:param name="roleId" value="${ model.role.id }" />
						</spring:url>
						<a id="userConfig${ status.count }" href="${ fn:escapeXml(userConfigUrl) }">Configure Users</a> 
					</td>
				</tr>
			</c:forEach>
		</tbody>
	</table>
	<br/>
	<a id="createNewRoleLink" href="<spring:url value="/configuration/roles/new" />">Create New Role</a> |
	<a id="backToMenuLink" href="<spring:url value="/configuration" />">Back to Configuration Index</a>
	
</body>