import { expect, test } from '@drownek/paperwright';

test('Titan loads on Paper and registers its commands', async ({ player, server }) => {
  server.execute('plugins');
  await expect(server).toHaveReceivedMessage('TitanMC');

  await player.makeOp();
  player.chat('/mine list');

  await expect(player).toHaveReceivedMessage('There are no mines yet.');
});
