////////////////////////////////////////////////////////////////////////
//
//     Copyright (c) 2009-2013 Denim Group, Ltd.
//
//     The contents of this file are subject to the Mozilla Public License
//     Version 2.0 (the "License"); you may not use this file except in
//     compliance with the License. You may obtain a copy of the License at
//     http://www.mozilla.org/MPL/
//
//     Software distributed under the License is distributed on an "AS IS"
//     basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
//     License for the specific language governing rights and limitations
//     under the License.
//
//     The Original Code is ThreadFix.
//
//     The Initial Developer of the Original Code is Denim Group, Ltd.
//     Portions created by Denim Group, Ltd. are Copyright (C)
//     Denim Group, Ltd. All Rights Reserved.
//
//     Contributor(s): Denim Group, Ltd.
//
////////////////////////////////////////////////////////////////////////
package com.denimgroup.threadfix.service.framework;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.denimgroup.threadfix.data.entities.DataFlowElement;
import com.denimgroup.threadfix.data.entities.Finding;

public class SpringParameterParsingTests {
	
	// These are immutable so it's ok to use the same one for all the tests
	SpringModelParameterParser parser = new SpringModelParameterParser();
	
	@Test
	public void testBasicModelParsing() {
		
		// These are from the PetClinic Fortify results
		List<DataFlowElement> basicModelElements = Arrays.asList(
			new DataFlowElement("java/org/springframework/samples/petclinic/web/OwnerController.java",85,
					"public String processFindForm(Owner owner, BindingResult result, Model model) {"),
			new DataFlowElement("java/org/springframework/samples/petclinic/web/OwnerController.java", 93,
					"Collection<Owner> results = this.clinicService.findOwnerByLastName(owner.getLastName());"),
			new DataFlowElement("java/org/springframework/samples/petclinic/web/OwnerController.java", 93,
					"Collection<Owner> results = this.clinicService.findOwnerByLastName(owner.getLastName());"),
			new DataFlowElement("java/org/springframework/samples/petclinic/service/ClinicServiceImpl.java", 72,
					"return ownerRepository.findByLastName(lastName);"),
			new DataFlowElement("java/org/springframework/samples/petclinic/repository/jdbc/JdbcOwnerRepositoryImpl.java", 84,
					"\"SELECT id, first_name, last_name, address, city, telephone FROM owners WHERE last_name like '\" + lastName + \"%'\",")
			);
		
		Finding finding = new Finding();
		finding.setDataFlowElements(basicModelElements);
		
		assertTrue("lastName".equals(parser.parse(finding)));
	}
	
	@Test
	public void testChainedModelParsing() {
		
		// These are doctored to test a corner case
		List<DataFlowElement> chainedModelElements = Arrays.asList(
			new DataFlowElement("java/org/springframework/samples/petclinic/web/OwnerController.java",85,
					"public String processFindForm(Pet pet, BindingResult result, Model model) {"),
			new DataFlowElement("java/org/springframework/samples/petclinic/web/OwnerController.java", 93,
					"Collection<Owner> results = this.clinicService.findOwnerByLastName(pet.getOwner().getLastName());"),
			new DataFlowElement("java/org/springframework/samples/petclinic/web/OwnerController.java", 93,
					"Collection<Owner> results = this.clinicService.findOwnerByLastName(pet.getOwner().getLastName());"),
			new DataFlowElement("java/org/springframework/samples/petclinic/service/ClinicServiceImpl.java", 72,
					"return ownerRepository.findByLastName(lastName);"),
			new DataFlowElement("java/org/springframework/samples/petclinic/repository/jdbc/JdbcOwnerRepositoryImpl.java", 84,
					"\"SELECT id, first_name, last_name, address, city, telephone FROM owners WHERE last_name like '\" + lastName + \"%'\",")
			);
		
		Finding finding = new Finding();
		finding.setDataFlowElements(chainedModelElements);
		
		assertTrue("owner.lastName".equals(parser.parse(finding)));
	}
	
	@Test
	public void testChainedMultiLevelModelParsing() {
		
		// These are doctored to test a corner case
		List<DataFlowElement> chainedMultiLevelModelElements = Arrays.asList(
			new DataFlowElement("java/org/springframework/samples/petclinic/web/OwnerController.java",85,
				"public String processFindForm(Pet pet, BindingResult result, Model model) {"),
			new DataFlowElement("java/org/springframework/samples/petclinic/web/OwnerController.java", 93,
				"Collection<Owner> results = this.clinicService.findOwnerByLastName(pet.getOwner());"),
			new DataFlowElement("java/org/springframework/samples/petclinic/web/OwnerController.java", 93,
				"Collection<Owner> results = this.clinicService.findOwnerByLastName(pet.getOwner());"),
			new DataFlowElement("java/org/springframework/samples/petclinic/service/ClinicServiceImpl.java", 72,
				"return ownerRepository.findByLastName(owner.getLastName());"),
			new DataFlowElement("java/org/springframework/samples/petclinic/repository/jdbc/JdbcOwnerRepositoryImpl.java", 84,
				"\"SELECT id, first_name, last_name, address, city, telephone FROM owners WHERE last_name like '\" + lastName + \"%'\",")
			);
		
		Finding finding = new Finding();
		finding.setDataFlowElements(chainedMultiLevelModelElements);
		
		assertTrue("owner.lastName".equals(parser.parse(finding)));
	}
	
	@Test
	public void testRequestParamParsing1() {
		
		// These are doctored to test other methods of passing Spring parameters
		List<DataFlowElement> chainedRequestParamElements1 = Arrays.asList(
			new DataFlowElement("java/org/springframework/samples/petclinic/web/OwnerController.java",85,
				"public String processFindForm(@RequestParam(\"testParam\") String lastName, Model model) {"),
			new DataFlowElement("java/org/springframework/samples/petclinic/web/OwnerController.java", 93,
				"Collection<Owner> results = this.clinicService.findOwnerByLastName(lastName);"),
			new DataFlowElement("java/org/springframework/samples/petclinic/service/ClinicServiceImpl.java", 72,
				"return ownerRepository.findByLastName(lastName);"),
			new DataFlowElement("java/org/springframework/samples/petclinic/repository/jdbc/JdbcOwnerRepositoryImpl.java", 84,
				"\"SELECT id, first_name, last_name, address, city, telephone FROM owners WHERE last_name like '\" + lastName + \"%'\",")
			);
		
		Finding finding = new Finding();
		finding.setDataFlowElements(chainedRequestParamElements1);
		
		assertTrue("testParam".equals(parser.parse(finding)));
	}
	
	@Test
	public void testRequestParamParsing2() {
		
		// These are doctored to test other methods of passing Spring parameters
		List<DataFlowElement> chainedRequestParamElements2 = Arrays.asList(
			new DataFlowElement("java/org/springframework/samples/petclinic/web/OwnerController.java",85,
				"public String processFindForm(@RequestParam String lastName, Model model) {"),
			new DataFlowElement("java/org/springframework/samples/petclinic/web/OwnerController.java", 93,
				"Collection<Owner> results = this.clinicService.findOwnerByLastName(lastName);"),
			new DataFlowElement("java/org/springframework/samples/petclinic/service/ClinicServiceImpl.java", 72,
				"return ownerRepository.findByLastName(lastName);"),
			new DataFlowElement("java/org/springframework/samples/petclinic/repository/jdbc/JdbcOwnerRepositoryImpl.java", 84,
				"\"SELECT id, first_name, last_name, address, city, telephone FROM owners WHERE last_name like '\" + lastName + \"%'\",")
			);
		
		Finding finding = new Finding();
		finding.setDataFlowElements(chainedRequestParamElements2);
		
		assertTrue("lastName".equals(parser.parse(finding)));
	}
	
	@Test
	public void testPathVariableParsing1() {
		
		// These are doctored to test other methods of passing Spring parameters
		List<DataFlowElement> chainedPathVariableElements1 = Arrays.asList(
			new DataFlowElement("java/org/springframework/samples/petclinic/web/OwnerController.java",85,
				"public String processFindForm(@PathVariable(\"testParam\") String lastName, Model model) {"),
			new DataFlowElement("java/org/springframework/samples/petclinic/web/OwnerController.java", 93,
				"Collection<Owner> results = this.clinicService.findOwnerByLastName(lastName);"),
			new DataFlowElement("java/org/springframework/samples/petclinic/service/ClinicServiceImpl.java", 72,
				"return ownerRepository.findByLastName(lastName);"),
			new DataFlowElement("java/org/springframework/samples/petclinic/repository/jdbc/JdbcOwnerRepositoryImpl.java", 84,
				"\"SELECT id, first_name, last_name, address, city, telephone FROM owners WHERE last_name like '\" + lastName + \"%'\",")
			);
		
		Finding finding = new Finding();
		finding.setDataFlowElements(chainedPathVariableElements1);
		
		assertTrue("testParam".equals(parser.parse(finding)));
	}
	
	@Test
	public void testPathVariableParsing2() {
		
		// These are doctored to test other methods of passing Spring parameters
		List<DataFlowElement> pathVariableElements2 = Arrays.asList(
			new DataFlowElement("java/org/springframework/samples/petclinic/web/OwnerController.java",85,
				"public String processFindForm(@PathVariable String lastName, Model model) {"),
			new DataFlowElement("java/org/springframework/samples/petclinic/web/OwnerController.java", 93,
				"Collection<Owner> results = this.clinicService.findOwnerByLastName(lastName);"),
			new DataFlowElement("java/org/springframework/samples/petclinic/service/ClinicServiceImpl.java", 72,
				"return ownerRepository.findByLastName(lastName);"),
			new DataFlowElement("java/org/springframework/samples/petclinic/repository/jdbc/JdbcOwnerRepositoryImpl.java", 84,
				"\"SELECT id, first_name, last_name, address, city, telephone FROM owners WHERE last_name like '\" + lastName + \"%'\",")
			);
		
		Finding finding = new Finding();
		finding.setDataFlowElements(pathVariableElements2);
		
		assertTrue("lastName".equals(parser.parse(finding)));
	}

}