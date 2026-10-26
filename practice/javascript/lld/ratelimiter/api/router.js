const express = require('express')

const router = express.Router()

router.post('/take',
  (req, res, next) => {
    req.container.takeApi.handleRequest(req, res).catch(next)
  }
)

module.exports = router
