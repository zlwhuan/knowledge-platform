# OnlyOffice Offline Preview Setup

This project now supports an Office preview route based on OnlyOffice Document Server.

## 1) Start OnlyOffice Document Server

Use Docker on the same LAN/VPS:

```bash
docker run -d --name onlyoffice-document-server -p 8088:80 onlyoffice/documentserver
```

## 2) Configure backend

Edit `src/main/resources/application.properties`:

```properties
preview.onlyoffice.enabled=true
preview.onlyoffice.server-url=http://127.0.0.1:8088
preview.onlyoffice.public-base-url=http://127.0.0.1:8080
```

- `preview.onlyoffice.server-url`: URL of OnlyOffice Document Server
- `preview.onlyoffice.public-base-url`: public URL that OnlyOffice can use to download attachments from this backend

If deployed behind a gateway/domain, replace localhost with reachable hostnames.

## 3) Verify

1. Upload a `.docx`/`.xlsx`/`.pptx` attachment
2. Open preview in UI
3. Backend should return `kind=onlyoffice` and a preview URL to OnlyOffice

If OnlyOffice is unavailable, backend automatically falls back to LibreOffice/PDF and then text preview.
