/*
 * Copyright 2016 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.appengine.deploy.validators

import com.netflix.spinnaker.clouddriver.appengine.security.AppEngineNamedAccountCredentials
import com.netflix.spinnaker.clouddriver.security.DefaultAccountCredentialsProvider
import com.netflix.spinnaker.clouddriver.security.MapBackedAccountCredentialsRepository
import org.springframework.validation.Errors
import spock.lang.Shared
import spock.lang.Specification

class StandardAppEngineAttributeValidatorSpec extends Specification {
  private static final DECORATOR = "decorator"
  private static final ACCOUNT_NAME = "my-appengine-account"
  private static final APPLICATION_NAME = "test-app"
  private static final REGION = "us-central"

  @Shared
  DefaultAccountCredentialsProvider accountCredentialsProvider

  void setupSpec() {
    def credentialsRepo = new MapBackedAccountCredentialsRepository()
    accountCredentialsProvider = new DefaultAccountCredentialsProvider(credentialsRepo)

    def namedAccountCredentials = new AppEngineNamedAccountCredentials.Builder()
      .name(ACCOUNT_NAME)
      .region(REGION)
      .applicationName(APPLICATION_NAME)
      .build()

    credentialsRepo.save(ACCOUNT_NAME, namedAccountCredentials)
  }

  void "validate non-empty valid"() {
    setup:
      def errors = Mock(Errors)
      def validator = new StandardAppEngineAttributeValidator(DECORATOR, errors)
      def label = "attribute"

    when:
      validator.validateNotEmpty("something", label)
    then:
      0 * errors._

    when:
      validator.validateNotEmpty(["something"], label)
    then:
      0 * errors._

    when:
      validator.validateNotEmpty([""], label)
    then:
      0 * errors._

    when:
      validator.validateNotEmpty([null], label)
    then:
      0 * errors._
  }

  void "validate non-empty invalid"() {
    setup:
      def errors = Mock(Errors)
      def validator = new StandardAppEngineAttributeValidator(DECORATOR, errors)
      def label = "attribute"

    when:
      validator.validateNotEmpty(null, label)
    then:
      1 * errors.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.empty")

    when:
      validator.validateNotEmpty("", label)
    then:
      1 * errors.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.empty")

    when:
      validator.validateNotEmpty([], label)
    then:
      1 * errors.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.empty")
  }

  void "validate by regex valid"() {
    setup:
      def errors = Mock(Errors)
      def validator = new StandardAppEngineAttributeValidator(DECORATOR, errors)
      def label = "attribute"

    when:
      validator.validateByRegex("app-engine", label, /\w{3}-\w{6}/)
    then:
      0 * errors._
  }

  void "validate by regex invalid"() {
    setup:
    def errors = Mock(Errors)
    def validator = new StandardAppEngineAttributeValidator(DECORATOR, errors)
    def label = "attribute"
    def regex = /\w{3}-\w{6}/

    when:
      validator.validateByRegex("app-engine-flex", label, regex)
    then:
      1 * errors.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.invalid (Must match ${regex})")
  }

  void "credentials reject (empty)"() {
    setup:
      def errorsMock = Mock(Errors)
      def validator = new StandardAppEngineAttributeValidator(DECORATOR, errorsMock)

    when:
      validator.validateCredentials(null, accountCredentialsProvider)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.account", "${DECORATOR}.account.empty")
      0 * errorsMock._

    when:
      validator.validateCredentials("", accountCredentialsProvider)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.account", "${DECORATOR}.account.empty")
      0 * errorsMock._
  }

  void "credentials reject (unknown)"() {
    setup:
      def errorsMock = Mock(Errors)
      def validator = new StandardAppEngineAttributeValidator(DECORATOR, errorsMock)

    when:
      validator.validateCredentials("You-don't-know-me", accountCredentialsProvider)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.account", "${DECORATOR}.account.notFound")
      0 * errorsMock._
  }

  void "credentials accept"() {
    setup:
      def errorsMock = Mock(Errors)
      def validator = new StandardAppEngineAttributeValidator(DECORATOR, errorsMock)

    when:
      validator.validateCredentials(ACCOUNT_NAME, accountCredentialsProvider)
    then:
      0 * errorsMock._
  }

  void "details accept"() {
    setup:
      def errorsMock = Mock(Errors)
      def validator = new StandardAppEngineAttributeValidator(DECORATOR, errorsMock)
      def label = "label"

    when:
      validator.validateDetails("valid", label)
    then:
      0 * errorsMock._

    when:
      validator.validateDetails("also-valid", label)
    then:
      0 * errorsMock._

    when:
      validator.validateDetails("123-456-789", label)
    then:
    0 * errorsMock._

    when:
      validator.validateDetails("", label)
    then:
      0 * errorsMock._
  }

  void "details reject"() {
    setup:
      def errorsMock = Mock(Errors)
      def validator = new StandardAppEngineAttributeValidator(DECORATOR, errorsMock)
      def label = "label"

    when:
      validator.validateDetails("-", label)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.invalid (Must match ${StandardAppEngineAttributeValidator.namePattern})")
      0 * errorsMock._

    when:
      validator.validateDetails("a space", label)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.invalid (Must match ${StandardAppEngineAttributeValidator.namePattern})")
      0 * errorsMock._

    when:
      validator.validateDetails("bad*details", label)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.invalid (Must match ${StandardAppEngineAttributeValidator.namePattern})")
      0 * errorsMock._

    when:
      validator.validateDetails("-k-e-b-a-b-", label)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.invalid (Must match ${StandardAppEngineAttributeValidator.namePattern})")
      0 * errorsMock._
  }

  void "application accept"() {
    setup:
      def errorsMock = Mock(Errors)
      def validator = new StandardAppEngineAttributeValidator(DECORATOR, errorsMock)
      def label = "label"

    when:
      validator.validateApplication("valid", label)
    then:
      0 * errorsMock._

    when:
      validator.validateApplication("application", label)
    then:
      0 * errorsMock._

    when:
      validator.validateApplication("7890", label)
    then:
      0 * errorsMock._
  }

  void "application reject"() {
    setup:
      def errorsMock = Mock(Errors)
      def validator = new StandardAppEngineAttributeValidator(DECORATOR, errorsMock)
      def label = "label"

    when:
      validator.validateApplication("l-l", label)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.invalid (Must match ${StandardAppEngineAttributeValidator.prefixPattern})")
      0 * errorsMock._

    when:
      validator.validateApplication("?application", label)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.invalid (Must match ${StandardAppEngineAttributeValidator.prefixPattern})")
      0 * errorsMock._

    when:
      validator.validateApplication("", label)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.empty")
      0 * errorsMock._
  }

  void "stack accept"() {
    setup:
      def errorsMock = Mock(Errors)
      def validator = new StandardAppEngineAttributeValidator(DECORATOR, errorsMock)
      def label = "label"

    when:
      validator.validateStack("valid", label)
    then:
      0 * errorsMock._

    when:
      validator.validateStack("stack", label)
    then:
      0 * errorsMock._

    when:
      validator.validateStack("7890", label)
    then:
      0 * errorsMock._
  }

  void "stack reject"() {
    setup:
      def errorsMock = Mock(Errors)
      def validator = new StandardAppEngineAttributeValidator(DECORATOR, errorsMock)
      def label = "label"

    when:
      validator.validateStack("l-l", label)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.invalid (Must match ${StandardAppEngineAttributeValidator.prefixPattern})")
      0 * errorsMock._

    when:
      validator.validateStack("?stack", label)
    then:
      1 * errorsMock.rejectValue("${DECORATOR}.${label}", "${DECORATOR}.${label}.invalid (Must match ${StandardAppEngineAttributeValidator.prefixPattern})")
      0 * errorsMock._
  }
}
