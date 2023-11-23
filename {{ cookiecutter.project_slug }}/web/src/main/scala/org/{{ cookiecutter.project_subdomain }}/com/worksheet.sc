import sttp.client3.{HttpURLConnectionBackend, UriContext, basicRequest}

import java.util.UUID

val payload =
  """{
    |  "key": "PARTNER_USER_INFO_{random_duaid}_{random_use_id}",
    |  "timeToLive": 86400,
    |  "value": {
    |    "ProfileInfos": [
    |      {
    |        "UniqueId": "7c841c5c-b337-42c6-8bb4-fd4b46657f89",
    |        "ProductInternalNumber": null,
    |        "Name": {
    |          "FirstName": "John",
    |          "LastName": "Doe",
    |          "MiddleName": ""
    |        },
    |        "EmailAddress": {
    |          "str": "DPSXforceWPLUS@expediagroup.com",
    |          "Email": "DPSXforceWPLUS@expediagroup.com"
    |        },
    |        "Address": null,
    |        "Type": null,
    |        "DOB": null,
    |        "Phone": null,
    |        "NewProfile": false
    |      }
    |    ],
    |    "AccountLevelInfo": {
    |      "channelMobile": false,
    |      "ProgramInfo": {
    |        "lowestFareRuleApplicable": false,
    |        "RateId": "",
    |        "ProgramId": "WPLUS",
    |        "ProgramName": "WPLUS",
    |        "ProgramGroupId": null,
    |        "ProgramType": null,
    |        "FeeConversionRatio": null,
    |        "NonRedeemableStatus": null,
    |        "ProfitCenterId": null,
    |        "CpcCode": null,
    |        "ProgramTier": null,
    |        "Rate": 0.01,
    |        "BaseBurnRate": null,
    |        "FlightBurnRate": null,
    |        "EarnRate": null,
    |        "HasVisaSignature": false,
    |        "IsLowestFareRuleApplicable": false,
    |        "SupportPhones": null,
    |        "IssuerId": null,
    |        "IssuerName": null,
    |        "cardProductName": null
    |      },
    |      "AccountBalance": "0",
    |      "loyaltyCashEquivalent": null,
    |      "ProfileIDs": null,
    |      "TaxProvinceCode": null,
    |      "ProductAccountNumber": null,
    |      "ProductSubCode": null,
    |      "RewardsId": "7c841c5c-b337-42c6-8bb4-fd4b46657f89",
    |      "ResidenceStateCode": null,
    |      "AccountBalanceCurrencyType": null,
    |      "ProgramType": null,
    |      "CardType": null,
    |      "LastFourNumbersOfCreditCard": null,
    |      "ProductCode": null,
    |      "AccountNumber": null,
    |      "AccountIdentifier": null,
    |      "Language": null,
    |      "LoginMethod": "",
    |      "ClientType": null,
    |      "AccountType": null,
    |      "ShortNumber": null,
    |      "SurrogateId": null,
    |      "channel": null,
    |      "IsChannelMobile": false,
    |      "AccountBalanceExpiryDate": null,
    |      "AccountStatus": null,
    |      "travelBenefitInfo": null
    |    },
    |    "travelEquation": null,
    |    "FiTransparencyIndicator": null,
    |    "GroupCode": null,
    |    "RedeemStatus": null,
    |    "isSpecialProgram": null,
    |    "Token": null,
    |    "PosCode": "null",
    |    "ProductType": null,
    |    "pointsPaymentInstrumentId": null,
    |    "customerPaymentInstrumentId": null,
    |    "pointsPaymentSubmethod": null,
    |    "VendorChainInfo": null,
    |    "accountIndex": null,
    |    "digitalProfileId": null,
    |    "gwsSessionCacheId": null,
    |    "LandingPageURL": null,
    |    "PartnerCallbackUrl": null,
    |    "LogoutRedirectionUrl": null,
    |    "PartnerSessionKeepAliveUrl": null,
    |    "PartnerEnvironmentIndicator": null,
    |    "ssoSamlXmlData": null,
    |    "ProgramAccounts": null,
    |    "BrandName": null,
    |    "Site": null,
    |    "additionalData": {}
    |  }
    |}""".stripMargin

val rand    = new scala.util.Random
val backend = HttpURLConnectionBackend()

for (a <- 1 to 50) {
  val duaid  = UUID.randomUUID().toString
  val userId = rand.nextInt().abs

  val newPayload = payload.replace("{random_duaid}", duaid).replace("{random_use_id}", userId.toString)
  val id         = s"PARTNER_USER_INFO_${duaid}_$userId"
  val request = basicRequest
    .post(
      uri"https://partner-loyalty-user-service.rcp.demandsolutions.prod-cts.exp-aws.net/service/v1/storePartnerUser"
    )
    .body(newPayload)
    .header("Content-Type", "application/json")

  val response = request.send(backend)
  println(s"$id - ${response.code}")

}