const chai = require('chai')
const chaiHttp = require('chai-http')
const server = require('../../server')
const { buildContainer } = require('../../di')

chai.use(chaiHttp)

const { request, expect } = chai

describe('POST /take', () => {
  const rateLimitConfig = { rateLimitsPerEndpoint: [{ endpoint: 'GET /user/:id', burst: 2, sustained: 6 }] }

  let app

  beforeEach(() => {
    app = server(buildContainer({ rateLimitConfig }))
  })

  function take(body) {
    return request(app).post('/take').send(body)
  }

  it('accepts a valid request', async () => {
    const response = await take({ endpoint: 'GET /user/:id', clientId: 'user-1' })

    expect(response.status).to.equal(200)
    expect(response.body).to.deep.equal({ accepted: true, remainingTokens: 1, retryAfterMs: 0 })
  })

  it('returns 404 for an unknown endpoint', async () => {
    const response = await take({ endpoint: 'GET /unknown' })

    expect(response.status).to.equal(404)
  })

  it('returns 400 when endpoint is missing', async () => {
    const response = await take({})

    expect(response.status).to.equal(400)
    expect(response.body.error).to.equal('"endpoint" is required')
  })

  it('returns 400 for an empty endpoint', async () => {
    const response = await take({ endpoint: '' })

    expect(response.status).to.equal(400)
  })

  it('returns 400 for an empty client id', async () => {
    const response = await take({ endpoint: 'GET /user/:id', clientId: '' })

    expect(response.status).to.equal(400)
  })

  it('returns 400 for a non-string client id', async () => {
    const response = await take({ endpoint: 'GET /user/:id', clientId: 42 })

    expect(response.status).to.equal(400)
  })

  it('returns 400 for unknown fields', async () => {
    const response = await take({ endpoint: 'GET /user/:id', userName: 'john' })

    expect(response.status).to.equal(400)
    expect(response.body.error).to.equal('"userName" is not allowed')
  })
})
