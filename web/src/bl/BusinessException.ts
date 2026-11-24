// [BL] BusinessException - invalid input or broken business rule (HTTP 400)
export class BusinessException extends Error {
  readonly errors: string[];
  constructor(errors: string | string[]) {
    const list = Array.isArray(errors) ? errors : [errors];
    super(list.join('\n'));
    this.errors = list;
  }
}

// [BL] AuthException - not logged in / wrong role (HTTP 401 / 403)
export class AuthException extends Error {
  constructor(message: string, readonly status: 401 | 403 = 401) { super(message); }
}
