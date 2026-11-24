/** GharBaar web - Next.js configuration */
const nextConfig = {
  reactStrictMode: true,
  serverExternalPackages: ['pg'],
  experimental: { serverActions: { bodySizeLimit: '5mb' } },
};

export default nextConfig;
