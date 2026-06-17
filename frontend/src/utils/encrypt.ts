const DEFAULT_SECRET_KEY = 'pay_gateway_hub_secret_key_2024';

const utf8ToBase64 = (str: string): string => {
  return btoa(unescape(encodeURIComponent(str)));
};

const base64ToUtf8 = (str: string): string => {
  return decodeURIComponent(escape(atob(str)));
};

const xorEncrypt = (text: string, key: string): string => {
  let result = '';
  for (let i = 0; i < text.length; i++) {
    result += String.fromCharCode(text.charCodeAt(i) ^ key.charCodeAt(i % key.length));
  }
  return result;
};

export const encrypt = (plainText: string, secretKey: string = DEFAULT_SECRET_KEY): string => {
  try {
    const encrypted = xorEncrypt(plainText, secretKey);
    return utf8ToBase64(encrypted);
  } catch {
    return plainText;
  }
};

export const decrypt = (cipherText: string, secretKey: string = DEFAULT_SECRET_KEY): string => {
  try {
    const decoded = base64ToUtf8(cipherText);
    return xorEncrypt(decoded, secretKey);
  } catch {
    return cipherText;
  }
};

export const md5 = async (text: string): Promise<string> => {
  const encoder = new TextEncoder();
  const data = encoder.encode(text);
  const hashBuffer = await crypto.subtle.digest('SHA-256', data);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map((b) => b.toString(16).padStart(2, '0')).join('');
};

export const generateSign = async (params: Record<string, unknown>, secretKey: string = DEFAULT_SECRET_KEY): Promise<string> => {
  const sortedKeys = Object.keys(params).sort();
  const signStr = sortedKeys
    .filter((key) => params[key] !== undefined && params[key] !== null && params[key] !== '')
    .map((key) => `${key}=${params[key]}`)
    .join('&') + `&key=${secretKey}`;
  return md5(signStr);
};

export const maskSensitive = (value: string, type: 'phone' | 'idCard' | 'bankCard' | 'email' = 'phone'): string => {
  if (!value) return '';
  switch (type) {
    case 'phone':
      return value.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2');
    case 'idCard':
      return value.replace(/(\d{4})\d{10}(\d{4})/, '$1**********$2');
    case 'bankCard':
      return value.replace(/(\d{4})\d+(\d{4})/, '$1 **** **** $2');
    case 'email': {
      const [name, domain] = value.split('@');
      if (!name || !domain) return value;
      const maskedName = name.length > 2 ? name.substring(0, 2) + '*'.repeat(name.length - 2) : name;
      return `${maskedName}@${domain}`;
    }
    default:
      return value;
  }
};
