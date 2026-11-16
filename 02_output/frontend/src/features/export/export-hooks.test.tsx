import { act, renderHook } from '@testing-library/react';
import type { ReactNode } from 'react';
import { Services } from '@/app/services';
import { ApiError } from '@/lib/api/client';
import { useExport } from './useExport';
import { useSelection } from './useSelection';

function wrapperFor(download: ReturnType<typeof vi.fn>) {
  const services = { api: { download, request: vi.fn() }, auth: {} } as never;
  return ({ children }: { children: ReactNode }) => <Services value={services}>{children}</Services>;
}

describe('useExport', () => {
  beforeEach(() => {
    URL.createObjectURL = vi.fn(() => 'blob:x');
    URL.revokeObjectURL = vi.fn();
  });

  it('downloads the file under its name through a temporary link, then is done (R-011-04)', async () => {
    const blob = new Blob(['x']);
    const download = vi.fn(async () => ({ blob, fileName: 'a b.xlsx' }));
    const clicked: HTMLAnchorElement[] = [];
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (
      this: HTMLAnchorElement,
    ) {
      clicked.push(this);
      expect(document.body.contains(this)).toBe(true);
    });
    const { result } = renderHook(() => useExport('g 1'), { wrapper: wrapperFor(download) });
    expect(result.current.state).toBe('idle');
    await act(async () => {
      await result.current.run({ language: 'en' } as never);
    });
    expect(download).toHaveBeenCalledWith('/groups/g%201/exports', { language: 'en' });
    expect(click).toHaveBeenCalledTimes(1);
    expect(clicked[0]?.download).toBe('a b.xlsx');
    expect(clicked[0]?.href).toBe('blob:x');
    expect(document.body.contains(clicked[0]!)).toBe(false);
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:x');
    expect(result.current.state).toBe('done');
    act(() => result.current.reset());
    expect(result.current.state).toBe('idle');
    click.mockRestore();
  });

  it('is busy while the request runs', async () => {
    let finish: (v: { blob: Blob; fileName: string }) => void = () => undefined;
    const download = vi.fn(() => new Promise<{ blob: Blob; fileName: string }>((r) => (finish = r)));
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
    const { result } = renderHook(() => useExport('g1'), { wrapper: wrapperFor(download) });
    let pending: Promise<void> = Promise.resolve();
    act(() => {
      pending = result.current.run({} as never);
    });
    expect(result.current.state).toBe('busy');
    await act(async () => {
      finish({ blob: new Blob(['x']), fileName: 'f.xlsx' });
      await pending;
    });
    expect(result.current.state).toBe('done');
  });

  it('tells too many contacts from any other failure (R-011-09)', async () => {
    const tooMany = new ApiError(400, {
      status: 400,
      code: 'VALIDATION_FAILED',
      message: 'x',
      errors: [{ field: 'ids', code: 'TOO_MANY_CONTACTS' }],
    } as never);
    const other = new ApiError(400, {
      status: 400,
      code: 'VALIDATION_FAILED',
      message: 'x',
      errors: [
        { field: 'ids', code: 'OTHER' },
        { field: 'ids', code: 'TOO_MANY_CONTACTS' },
      ],
    } as never);
    const mixed = new ApiError(400, {
      status: 400,
      code: 'VALIDATION_FAILED',
      message: 'x',
      errors: [{ field: 'ids', code: 'OTHER' }],
    } as never);
    for (const [failure, expected] of [
      [tooMany, 'tooMany'],
      [other, 'tooMany'],
      [mixed, 'error'],
      [new Error('network'), 'error'],
    ] as const) {
      const download = vi.fn(async () => {
        throw failure;
      });
      const { result } = renderHook(() => useExport('g1'), { wrapper: wrapperFor(download) });
      await act(async () => {
        await result.current.run({} as never);
      });
      expect(result.current.state).toBe(expected);
    }
  });
});

describe('useSelection', () => {
  it('ticks and unticks rows and keeps them across calls (R-011-01)', () => {
    const { result } = renderHook(() => useSelection());
    act(() => result.current.set(['a', 'b'], true, ['a', 'b', 'c']));
    expect([...result.current.ids]).toEqual(['a', 'b']);
    act(() => result.current.set(['a'], false, ['a', 'b', 'c']));
    expect([...result.current.ids]).toEqual(['b']);
    expect(result.current.all).toBe(false);
  });

  it('leaves "all results" when one row is unticked and keeps the rest of the page (R-011-02)', () => {
    const { result } = renderHook(() => useSelection());
    act(() => result.current.selectAll());
    expect(result.current.all).toBe(true);
    act(() => result.current.set(['b'], false, ['a', 'b', 'c']));
    expect(result.current.all).toBe(false);
    expect([...result.current.ids].sort()).toEqual(['a', 'c']);
  });

  it('clears the ids and "all results"', () => {
    const { result } = renderHook(() => useSelection());
    act(() => result.current.set(['a'], true, ['a']));
    act(() => result.current.selectAll());
    act(() => result.current.clear());
    expect(result.current.ids.size).toBe(0);
    expect(result.current.all).toBe(false);
  });
});
