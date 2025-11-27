const jsonServer = require('json-server');
const path = require('path');
const fs = require('fs');
const https = require('https');
const server = jsonServer.create();
const router = jsonServer.router('./bdi.json');
const middlewares = jsonServer.defaults();


const routes = require('./routes.json');

server.use(middlewares);

server.use(jsonServer.rewriter(routes));

server.get('/opi-rend-out/*', (req, res) => {

  const fixedFilePath = path.join(__dirname, '.', './test.zip.p7m');

  console.log(`Request: ${req.params[0]}`);

  // Download always the same file
  res.download(fixedFilePath, 'test.zip.p7m', (err) => {
    if (err) {
      console.error("Error during the download:", err);
      res.status(404).send("File not found");
    }
  });
});

server.use(router);

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

const httpsServer = https.createServer(options, server);

httpsServer.listen(3000, () => {
  console.log('Secure JSON Server (mTLS) running on port 3000');
});