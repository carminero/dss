package eu.europa.esig.dss.cades.validation;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import eu.europa.esig.dss.DSSDocument;
import eu.europa.esig.dss.InMemoryDocument;
import eu.europa.esig.dss.SignatureAlgorithm;
import eu.europa.esig.dss.SignatureLevel;
import eu.europa.esig.dss.SignaturePackaging;
import eu.europa.esig.dss.SignatureValue;
import eu.europa.esig.dss.ToBeSigned;
import eu.europa.esig.dss.EN319102.report.Reports;
import eu.europa.esig.dss.EN319102.wrappers.DiagnosticData;
import eu.europa.esig.dss.EN319102.wrappers.TimestampWrapper;
import eu.europa.esig.dss.cades.CAdESSignatureParameters;
import eu.europa.esig.dss.cades.signature.CAdESService;
import eu.europa.esig.dss.test.TestUtils;
import eu.europa.esig.dss.test.gen.CertificateService;
import eu.europa.esig.dss.test.mock.MockPrivateKeyEntry;
import eu.europa.esig.dss.test.mock.MockTSPSource;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.validation.SignedDocumentValidator;

public class CAdESBExtendToLTACheckTimeStampIDTest {

	@Test
	public void test() throws Exception {
		DSSDocument documentToSign = new InMemoryDocument("Hello World".getBytes());

		CertificateService certificateService = new CertificateService();
		MockPrivateKeyEntry privateKeyEntry = certificateService.generateCertificateChain(SignatureAlgorithm.RSA_SHA256);

		CAdESSignatureParameters signatureParameters = new CAdESSignatureParameters();
		signatureParameters.bLevel().setSigningDate(new Date());
		signatureParameters.setSigningCertificate(privateKeyEntry.getCertificate());
		signatureParameters.setCertificateChain(privateKeyEntry.getCertificateChain());
		signatureParameters.setSignaturePackaging(SignaturePackaging.ENVELOPING);
		signatureParameters.setSignatureLevel(SignatureLevel.CAdES_BASELINE_B);

		CertificateVerifier certificateVerifier = new CommonCertificateVerifier();
		CAdESService service = new CAdESService(certificateVerifier);
		service.setTspSource(new MockTSPSource(certificateService.generateTspCertificate(SignatureAlgorithm.RSA_SHA1), new Date()));

		ToBeSigned toBeSigned = service.getDataToSign(documentToSign, signatureParameters);
		SignatureValue signatureValue = TestUtils.sign(signatureParameters.getSignatureAlgorithm(), privateKeyEntry, toBeSigned);
		DSSDocument signedDocument = service.signDocument(documentToSign, signatureParameters, signatureValue);

		signatureParameters.setSignatureLevel(SignatureLevel.CAdES_BASELINE_LTA);

		signedDocument = service.extendDocument(signedDocument, signatureParameters);

		SignedDocumentValidator validator = SignedDocumentValidator.fromDocument(signedDocument);
		validator.setCertificateVerifier(new CommonCertificateVerifier());

		Reports report = validator.validateDocument();
		report.print();
		DiagnosticData diagnostic = report.getDiagnosticData();
		String signatureId = diagnostic.getFirstSignatureId();
		for (TimestampWrapper wrapper : diagnostic.getTimestampList(signatureId)) {
			Assert.assertEquals(signatureId, wrapper.getSignedObjects().getSignedSignature().get(0).getId());
		}
	}
}
