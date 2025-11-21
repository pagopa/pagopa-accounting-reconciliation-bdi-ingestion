const jsonServer = require('json-server');
const path = require('path');
const server = jsonServer.create();
const router = jsonServer.router('./bdi.json');
const middlewares = jsonServer.defaults();


const routes = require('./routes.json');

server.use(middlewares);

server.use(jsonServer.rewriter(routes));

server.get('/a2a/download/tesoreria/opi-rend-out/*', (req, res) => {

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

server.listen(3000, () => {
  console.log('bdi-mock server listening on port 3000');
});