# Configuration : [person - law] GDPR

**Document history (please add a line when you edit the document)**

| Date       | Editor(s) | OpenSILEX version     | Comment           |
|------------|-----------|-----------------------|-------------------|
| 20/10/2023 | Yvan Roux | 1.1.0 Blazing Basalt  | Document creation |

# Table of contents

<!-- TOC -->
* [Configuration : [person - law] GDPR](#configuration--person---law-gdpr)
* [Table of contents](#table-of-contents)
* [How to use](#how-to-use)
  * [Set up config](#set-up-config)
  * [Put a PDF from your computer to a sever](#put-a-pdf-from-your-computer-to-a-sever)
    * [exemple](#exemple)
<!-- TOC -->

# How to use

- Create a PDF with your GDPR text for each available language in your instance
- Put these PDFs on the same server as your OpenSilex instance
- Set up config with the absolute path of each PDF, in relation to their language

If you have to change the config (to set a new path), then you have to restart your instance.

If you want to CHANGE (not set) the GDPR PDF, you can just give the new pdf the same path to the old one (same path and name), and changes will be immediately visible if you refresh the web interface, without restarting the instance.

## Set up config

| ⚠️ <br/>**warning** | In this context, the absolute path is NECESSARILY from the root, so it has to begin with /. <br/>Using paths beginning with ~ or relative paths (./) may cause issues on getting it from the OpenSilex instance. |
|---------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

In the security part of the config, write the absolute path of each PDF and map it to its language just as below.

PDFs should be on the same server as the OpenSilex instance.

exemple :
```yaml
security:
  gdprPdfPathsByLanguages:
    en : /home/phis/doc_en_anglais.pdf
    fr : /home/phis/doc_en_francais.pdf
```

## Put a PDF from your computer to a sever

All texts in {curly braces} are meant to be replaced

First, you have to get the ssh configuration to connect to the "target" server. running the following command from your terminal should work ```ssh {target}```

Once your configuration is ready, run the following command from your terminal
```shell
scp {path of your PDF}/{name of your PDF} @{target server}:{ABSOLUTE path for your PDF}/{futur name of your PDF}
```

### exemple

- On my computer, I have created a RGPD PDF file named rgpd.pdf in the Downloads repertory.
- I have the configuration to access my server with ssh under the name testserver.
- I want to save this file on my server with the name "rgpd_en.pdf", at this location : ~/opensilex-document/rgpd/rgpd_en.pdf

```shell
scp ~/Downloads/rgpd.pdf @testserver:/home/me/opensilex-document/rgpd/rgpd_en.pdf  
```