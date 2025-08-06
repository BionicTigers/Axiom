<script lang="ts">
  
</script>

<ul>
  <!-- <li class="electron-version">Axiom - <span class="not-connected">NOT CONNECTED</span></li> -->
  <!-- <li class="chrome-version">Interface v{versions.chrome}</li> -->
</ul>

<style>
  ul {
    position: absolute;
    bottom: 30px;
    margin: 0 auto;
    padding: 15px 0;
    font-family: 'Menlo', 'Lucida Console', monospace;
    display: inline-flex;
    overflow: hidden;
    align-items: center;
    border-radius: 22px;
    background-color: #202127;
    backdrop-filter: blur(24px);
  }

  ul li {
    display: block;
    float: left;
    border-right: 1px solid var(--ev-c-gray-1);
    padding: 0 20px;
    font-size: 14px;
    line-height: 14px;
    opacity: 0.8;
    &:last-child {
      border: none;
    }
  }
</style>
