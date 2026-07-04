/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  // 도커 이미지 최소화 — 실행에 필요한 파일만 담은 standalone 서버 산출
  output: "standalone",
};

export default nextConfig;
