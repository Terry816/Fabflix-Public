import type { CartItem } from '../types';

export const CART_UPDATED_EVENT = 'fabflix:cart-updated';

export function cartItemCount(items: CartItem[]) {
  return items.reduce((sum, item) => sum + item.quantity, 0);
}

export function announceCartUpdated() {
  window.dispatchEvent(new Event(CART_UPDATED_EVENT));
}
