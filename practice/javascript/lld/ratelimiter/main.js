const { buildContainer } = require('./di')
const server = require('./server')

const container = buildContainer()
const { port } = container.appConfig

server(container).listen(port, () => {
  console.log(`Rate limiter listening on port ${port}`)
})
