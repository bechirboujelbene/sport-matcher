local cjson = require "cjson"
local oidc = require "resty.openidc"

local domain = os.getenv("AUTH0_DOMAIN")
local audience = os.getenv("AUTH0_AUDIENCE")

ngx.req.clear_header("X-User-Id")
ngx.req.clear_header("X-Service-Token")

local function unauthorized()
  ngx.status = 401
  ngx.header.content_type = "application/json; charset=utf-8"
  ngx.say(cjson.encode({ error = "unauthorized" }))
  return ngx.exit(ngx.HTTP_UNAUTHORIZED)
end

if ngx.var.request_method == "OPTIONS" then
  ngx.status = 204
  return ngx.exit(204)
end

local function bearer_token()
  local authorization = ngx.var.http_authorization
  if not authorization then
    return nil
  end
  return authorization:match("^[Bb]earer%s+(.+)$")
end

-- Local development mode: accept "dev-<subject>" bearer tokens without an
-- Auth0 tenant. Only enabled via the AUTH_MODE environment variable; the
-- default keeps full JWT verification.
if os.getenv("AUTH_MODE") == "dev" then
  local dev_subject = nil
  local token = bearer_token()
  if token then
    dev_subject = token:match("^dev%-(.+)$")
  end
  if not dev_subject or dev_subject == "" then
    ngx.log(ngx.WARN, "Dev auth requires a 'dev-<subject>' bearer token")
    return unauthorized()
  end
  ngx.req.set_header("X-User-Id", dev_subject)
  return
end

if not domain or domain == "" or not audience or audience == "" then
  ngx.log(ngx.ERR, "Auth0 gateway configuration is incomplete")
  ngx.status = 500
  ngx.header.content_type = "application/json; charset=utf-8"
  ngx.say(cjson.encode({ error = "authentication_not_configured" }))
  return ngx.exit(ngx.HTTP_INTERNAL_SERVER_ERROR)
end

local function token_subject(token)
  if not token then
    return nil
  end
  local payload = token:match("^[^.]+%.([^.]+)%.")
  if not payload then
    return nil
  end
  payload = payload:gsub("-", "+"):gsub("_", "/")
  payload = payload .. string.rep("=", (4 - (#payload % 4)) % 4)
  local decoded = ngx.decode_base64(payload)
  if not decoded then
    return nil
  end
  local ok, claims = pcall(cjson.decode, decoded)
  return ok and claims.sub or nil
end

local opts = {
  discovery = string.format("https://%s/.well-known/openid-configuration", domain),
  token_signing_alg_values_expected = {"RS256"},
  accept_none_alg = false,
  client_id = audience,
  redirect_uri = "https://unused.local",
  ssl_verify = "yes"
}

local res, err = oidc.bearer_jwt_verify(opts)
if err then
  ngx.log(ngx.WARN, "JWT verification failed")
  return unauthorized()
end

local subject
if type(res) == "table" then
  if type(res.id_token) == "table" then
    subject = res.id_token.sub
  end
  subject = subject or res.sub
end
subject = subject or token_subject(bearer_token())

if not subject or subject == "" then
  ngx.log(ngx.WARN, "Verified JWT did not contain a subject")
  return unauthorized()
end

ngx.req.set_header("X-User-Id", subject)
