import { ApiError, api } from './client';

function jsonResponse(body: unknown, init: ResponseInit = {}) {
  const status = init.status ?? 200;
  return {
    ok: status >= 200 && status < 300,
    status,
    statusText: init.statusText ?? 'OK',
    headers: {
      get: (name: string) => (name.toLowerCase() === 'content-type' ? 'application/json' : null)
    },
    json: async () => body,
    text: async () => JSON.stringify(body)
  } as Response;
}

describe('api client', () => {
  afterEach(() => {
    jest.restoreAllMocks();
    delete (globalThis as Partial<typeof globalThis>).fetch;
  });

  it('posts login credentials as form data with cookies enabled', async () => {
    const fetchMock = jest.fn().mockResolvedValue(jsonResponse({ status: 'success', message: 'Login successful.' }));
    (globalThis as Partial<typeof globalThis>).fetch = fetchMock as unknown as typeof fetch;

    await expect(api.login('terry@test.com', 'password')).resolves.toMatchObject({
      status: 'success',
      message: 'Login successful.'
    });

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/login',
      expect.objectContaining({
        credentials: 'include',
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: expect.any(URLSearchParams)
      })
    );

    const body = fetchMock.mock.calls[0][1]?.body as URLSearchParams;
    expect(body.get('username')).toBe('terry@test.com');
    expect(body.get('password')).toBe('password');
  });

  it('omits empty query parameters', async () => {
    const fetchMock = jest.fn().mockResolvedValue(jsonResponse([]));
    (globalThis as Partial<typeof globalThis>).fetch = fetchMock as unknown as typeof fetch;

    await api.movies({
      title: 'Spider',
      year: '',
      page: 2,
      pageSize: 10,
      top20: false
    });

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/search?title=Spider&page=2&pageSize=10&top20=false',
      expect.objectContaining({ credentials: 'include' })
    );
  });

  it('throws ApiError with backend message on failed responses', async () => {
    (globalThis as Partial<typeof globalThis>).fetch = jest.fn().mockResolvedValue(
      jsonResponse({ status: 'fail', message: 'Incorrect password.' }, { status: 401, statusText: 'Unauthorized' })
    ) as unknown as typeof fetch;

    await expect(api.login('terry@test.com', 'bad-password')).rejects.toMatchObject({
      status: 401,
      message: 'Incorrect password.'
    });
  });
});
