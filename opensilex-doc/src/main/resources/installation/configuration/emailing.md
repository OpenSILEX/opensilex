# Configuration : [emailing]

**Document history (please add a line when you edit the document)**

| Date       | Editor(s) | OpenSILEX version     | Comment           |
|------------|-----------|-----------------------|-------------------|
| 07/07/2025 | Arnaud Charleroy | 1.4.9  | Document creation |

 
<!-- TOC -->
- [Configuration : \[emailing\] GDPR](#configuration--emailing-gdpr)
- [Email Service Configuration Documentation](#email-service-configuration-documentation)
  - [Overview](#overview)
  - [Configuration Structure](#configuration-structure)
  - [Configuration Parameters](#configuration-parameters)
    - [General Settings](#general-settings)
    - [SMTP Settings](#smtp-settings)
  - [Use Case Example: Forgot Password Feature](#use-case-example-forgot-password-feature)
    - [Scenario](#scenario)
    - [Process Flow](#process-flow)

<!-- TOC -->

 

# Email Service Configuration Documentation

## Overview

This configuration enables email sending functionality within the OpenSILEX platform. The email service is essential for user communication features such as password reset notifications or others.

## Configuration Structure

The email service configuration is located under the `security` section and provides SMTP server settings for sending emails.

```yaml
security:
  # Email service (EmailService)
  email:
    config:
      enable: true
      smtp:
        host: smtp.inrae.fr
        port: 587
        userId: "mtp-opensilex"
        userPassword: "password"
        sender: "mtp-opensilex@inrae.fr"
```

## Configuration Parameters

### General Settings
- **enable**: Boolean flag to activate/deactivate the email service
  - `true`: Email service is active
  - `false`: Email service is disabled (no emails will be sent)

### SMTP Settings
- **host**: SMTP server hostname
  - Example: `smtp.inrae.fr`
  - The mail server that will handle outgoing emails

- **port**: SMTP server port
  - `587`: Standard port for SMTP with STARTTLS encryption
  - `465`: Alternative port for SMTP with SSL/TLS

- **userId**: SMTP authentication username
  - The username used to authenticate with the SMTP server
  - Example: `mtp-opensilex`

- **userPassword**: SMTP authentication password
  - The password for the SMTP user account
  - **Security Note**: Store this securely and consider using environment variables

- **sender**: Default sender email address
  - The email address that will appear as the sender in outgoing emails
  - Example: `mtp-opensilex@inrae.fr`
  - Should match the authenticated user's email domain

## Use Case Example: Forgot Password Feature

### Scenario
When a user forgets their password, they can request a password reset through the application. The email service will send them a secure reset link.

### Process Flow

1. **User Request**: User clicks "Forgot Password" and enters their email address
2. **Token Generation**: System generates a secure, time-limited reset token
3. **Email Composition**: System creates a password reset email with the token
4. **Email Sending**: EmailService uses the configured SMTP settings to send the email


For common issues see [this documentation](../faq/emailing.md)