package ccm.test;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ccm.models.system.justin.JustinAgencyFile;
import ccm.models.system.justin.JustinAccused;
import ccm.models.system.justin.JustinCourtFile;
import ccm.models.business.BusinessCourtCaseData;
import ccm.models.business.BusinessCourtCaseMetadataData;
import ccm.models.business.BusinessCourtCaseAccused;
import ccm.models.system.dems.DemsCourtCaseData;
import ccm.models.system.dems.DemsFieldData;


public class JacksonObjectMapperJustin {

	public static void main(String[] args) throws IOException {

		//read json file data to String
		byte[] jsonData = Files.readAllBytes(Paths.get("sampleAgencyFile.json"));
		byte[] jsonCourtFileData = Files.readAllBytes(Paths.get("sampleCourtFile.json"));
		byte[] jsonAssignmentData = Files.readAllBytes(Paths.get("sampleCrownAssignments.json"));

		//create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();

		//convert json string to object
		JustinAgencyFile agencyFile = objectMapper.readValue(jsonData, JustinAgencyFile.class);
		JustinCourtFile courtFile = objectMapper.readValue(jsonData, JustinCourtFile.class);
		objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);


		StringWriter stringFile = new StringWriter();
		StringWriter stringFileC = new StringWriter();
		StringWriter stringFile2 = new StringWriter();
		StringWriter stringFile2C = new StringWriter();
		StringWriter stringFile3 = new StringWriter();

		objectMapper.writeValue(stringFile, agencyFile);
		objectMapper.writeValue(stringFileC, courtFile);
		System.out.println("JustinAgencyFile JSON is\n"+stringFile);
		System.out.println("JustinCourtFile JSON is\n"+stringFileC);


		BusinessCourtCaseData businessFile = new BusinessCourtCaseData(agencyFile);
		BusinessCourtCaseMetadataData businessCaseFile = new BusinessCourtCaseMetadataData(courtFile);

		objectMapper.writeValue(stringFile2, businessFile);
		System.out.println("\n\nBusinessCourtCaseData JSON is\n"+stringFile2);



		DemsCourtCaseData demsCaseFile = new DemsCourtCaseData(businessFile, businessCaseFile);

		objectMapper.writeValue(stringFile3, demsCaseFile);
		System.out.println("\n\nDemsCreateCourtCase JSON is\n"+stringFile3);




		stringFile.close();
		stringFile2.close();
		stringFile3.close();

	}

}
