const chai = require('chai')
const chaiHttp = require('chai-http')
const server = require('../server')
const { buildContainer } = require('../di')

chai.use(chaiHttp)

const { request, expect } = chai

describe('server', () => {
  describe('GET /health', () => {
    it('responds with ok', async () => {
      const app = server(buildContainer())

      const response = await request(app).get('/health')

      expect(response.status).to.equal(200)
      expect(response.body).to.deep.equal({ status: 'ok' })
    })
  })

  describe('invalid JSON', () => {
    it('responds with 400', async () => {
      const app = server(buildContainer())

      const response = await request(app)
        .post('/take')
        .set('Content-Type', 'application/json')
        .send('{ not json')

      expect(response.status).to.equal(400)
      expect(response.body.error).to.equal('Invalid JSON body')
    })
  })

  describe('unknown routes', () => {
    it('responds with 404', async () => {
      const app = server(buildContainer())

      const response = await request(app).get('/does-not-exist')

      expect(response.status).to.equal(404)
    })
  })

  describe('unexpected errors', () => {
    const originalConsoleError = console.error
    let loggedErrors

    beforeEach(() => {
      loggedErrors = []
      console.error = (...args) => {
        loggedErrors.push(args)
      }
    })

    afterEach(() => {
      console.error = originalConsoleError
    })

    it('logs the error and responds with 500', async () => {
      const failingRateLimiter = {
        hasEndpoint: () => true,
        take: () => {
          throw new Error('boom')
        },
      }
      const app = server(buildContainer({ rateLimiter: failingRateLimiter }))

      const response = await request(app).post('/take').send({ endpoint: 'GET /user/:id' })

      expect(response.status).to.equal(500)
      expect(response.body).to.deep.equal({ error: 'Something went wrong' })
      expect(loggedErrors).to.have.lengthOf(1)
      expect(loggedErrors[0][1].path).to.equal('/take')
      expect(loggedErrors[0][1].error).to.contain('boom')
    })
  })
})
