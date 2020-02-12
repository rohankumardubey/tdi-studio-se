---
version: 7.1.1
module: https://talend.poolparty.biz/coretaxonomy/42
product: https://talend.poolparty.biz/coretaxonomy/23
---

# TPS-3769

| Info             | Value |
| ---------------- | ---------------- |
| Patch Name       | Patch\_20200212\_TPS-3769\_v1-7.1.1|
| Release Date     | 2020-02-12 |
| Target Version   | 20181026\_1147-v7.1.1 |
| Product affected | Talend Studio |

## Introduction <!-- mandatory -->
This is a self-contained patch.

**NOTE**: For information on how to obtain this patch, reach out to your Support contact at Talend.

## Fixed issues <!-- mandatory -->


This patch contains the following fixes:

- TPS-3769 resolves tBigQueryUpload operator loses fractional seconds for datetime/timestamp fields(TDI-43632)


## Prerequisites <!-- mandatory -->


Consider the following requirements for your system:

- Talend Studio 7.1.1 must be installed.
- Patch\_20190408\_TPS-3010\_v1-7.1.1 must be installed.
- Patch\_20190218\_TPS-2912\_v1-7.1.1 must be installed.
- Patch\_20190717\_TPS-3225\_v1-7.1.1 must be installed.

## Installation <!-- mandatory -->


### Installing the patch using Software update <!-- if applicable -->

1) Logon TAC and switch to Configuration->Software Update, then enter the correct values and save referring to the documentation: https://help.talend.com/reader/f7Em9WV_cPm2RRywucSN0Q/j9x5iXV~vyxMlUafnDejaQ

2) Switch to Software update page, where the new patch will be listed. The patch can be downloaded from here into the nexus repository.

3) On Studio Side: Logon Studio with remote mode, on the logon page the Update button is displayed: click this button to install the patch.

### Installing the patch using Talend Studio <!-- if applicable -->

1) Create a folder named "patches" under your studio installer directory and copy the patch .zip file to this folder.

2) Restart your studio: a window pops up, then click OK to install the patch, or restart the commandline and the patch will be installed automatically.

### Installing the patch using Commandline <!-- if applicable -->

Execute the following commands:

1. Talend-Studio-win-x86_64.exe -nosplash -application org.talend.commandline.CommandLine -consoleLog -data commandline-workspace startServer -p 8002 --talendDebug
2. initRemote {tac_url} -ul {TAC login username} -up {TAC login password}
3. checkAndUpdate -tu {TAC login username} -tup {TAC login password}

## Uninstallation
Backup the Affected files list below. Uninstall the patch by restore the backup files.

## Affected files for this patch

The following files are installed by this patch:

- {Talend\_Studio\_path}/plugins/org.talend.designer.components.localprovider\_7.1.1.20181026\_1147/components/tBigQueryOutputBulk/tBigQueryOutputBulk\_main.javajet