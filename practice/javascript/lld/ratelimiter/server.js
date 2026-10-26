const express = require('express')
const router = require('./api/router')

function server(container) {
  const app = express()

  app.use(express.json())

  app.use((req, res, next) => {
    req.container = container
    next()
  })

  app.get('/health', (req, res) => {
    return res.status(200).json({ status: 'ok' })
  })

  app.use('/', router)

  app.use((err, req, res, next) => {
    if (err.type === 'entity.parse.failed') {
      return res.status(400).json({ error: 'Invalid JSON body' })
    }
    console.error('Unhandled error', {
      method: req.method,
      path: req.originalUrl,
      body: req.body,
      error: err.stack || err,
    })
    return res.status(500).json({ error: 'Something went wrong' })
  })

  return app
}

module.exports = server
