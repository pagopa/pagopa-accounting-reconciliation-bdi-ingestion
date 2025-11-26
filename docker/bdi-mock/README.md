# BDI MOCK SERVER 🏦

This mock is base on __json-server__ node package and __https__ for the configuration of the SSL connection
based on a x.509 certificate.

### SSL CONNECTION 🔐
For the configuration of the SSL connection, you can use a test x.509 certificate generated with testing information 
situated in the __cert__ directory, for the mock, and for the client who want to connect to the mock you can use the
certificate situated in the __cert_client__ directory.

If you want to generate your personal certificate you can use the following commands:

1: Generate the certificate that will be used by the mock under the __cert__ directory:
```shell
cd cert
openssl req -nodes -new -x509 -keyout bdi-mock.key -out bdi-mock.pem -days 365 -subj "/CN=localhost"
```
2: Generate the certificate used by the client under the __cert_client__ directory:
```shell
cd cert_client
openssl req -nodes -new -x509 -keyout client.pem -out client_complete.pem -days 365 -subj "/CN=localhost"
```
In this case will be only a single file with private and public key

The __server.json__ is using to the default certificate, you can change the file name to the certificate in the
__option__ object if necessary :

```code
const options = {
  // private test key
  key: fs.readFileSync(path.join(__dirname, './bdi-mock.key')),

  // public test cert
  cert: fs.readFileSync(path.join(__dirname, './bdi-mock.pem')),

  // Add the public cert of the client in the array of trusted ca
  // This will be the only trusted certificate from the mock
  ca: [
    fs.readFileSync(path.join(__dirname, './client.pem'))
  ],

  requestCert: true,
  rejectUnauthorized: true
};
```
### Running 🚀
Go under the base directory of the project and run the docker-compose file:
```shell
docker compose up bdi-service-mock
```

### Testing 🔍
Using curl:
```shell
curl --insecure --cert client.pem --key client.key https://localhost:8091/opi-rend-out
curl --insecure --cert client.pem --key client.key https://localhost:8091/opi-rend-out/testFile
```