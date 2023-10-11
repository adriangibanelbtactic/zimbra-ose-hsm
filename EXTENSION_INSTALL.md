# Install Zimbra OSE HSM Extension

## Warning

**These are developer instructions.**

For Admin installation instructions please check [README.md](README.md) instead.

## Requisites

You have succesfully built zimbra-ose-hsm using [EXTENSION_BUILD.md](EXTENSION_BUILD.md) instructions.

## Installation

Get `/opt/zimbra/conf/scripts/zimbra-ose-hsm/extension/zetahsm.jar` from your build machine and copy it to your production machine on `/tmp/zetahsm.jar` .

This needs to be run as the root user:

```
cp /tmp/zetahsm.jar /opt/zimbra/lib/ext/twofactorauth/zetahsm.jar
```

## Zimbra Mailbox restart

For the new Extension to be used the Zimbra Mailbox has to be restarted.

```
sudo su - zimbra -c 'zmmailboxdctl restart'
```
